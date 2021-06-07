package com.quantexa.example.taskloading.batch

import com.quantexa.acl.AclModel.{PrivilegeSetId, PrivilegeSetName}
import com.quantexa.example.taskloading.model.Model._
import com.quantexa.example.taskloading.rest.RequestBodyGeneration._
import com.quantexa.example.taskloading.rest.Requests._
import com.quantexa.example.taskloading.utils.TaskUtils._
import com.quantexa.example.taskloading.utils.GeneralUtils
import com.quantexa.explorer.tasks.api.Model.TaskListId
import com.quantexa.resolver.core.EntityGraphLite.LiteGraph
import com.quantexa.scoring.framework.execution.CaseClassMapConverters
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}
import com.quantexa.example.taskloading.batch.encoders.SparkEncoders._
import com.quantexa.example.taskloading.batch.utils.TaskSparkUtils._
import com.quantexa.example.taskloading.config.TaskLoadingConfig
import com.quantexa.example.taskloading.model.ProjectModel.{ExtraData, OutcomeData, TaskLoadSuccess}
import com.quantexa.example.taskloading.rest.HttpClients

import scala.concurrent.duration.{FiniteDuration, SECONDS}

object LoadTasks extends TypedSparkScript[TaskLoadingConfig] {

  def name: String = "Load Tasks"

  def fileDependencies: Map[String, String] = Map.empty

  def scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], config: TaskLoadingConfig, etlMetricsRepository: ETLMetricsRepository) = {

    import spark.implicits._

    /*--------------- CONFIG AND PATHS ---------------*/

    val tasksRoot = config.tasksRoot
    val tasksThisRunRoot = tasksRoot

    //Tasks To Load Path
    val inputTasksToLoadPath = s"$tasksThisRunRoot/TasksToLoadThisRun.parquet"

    //Additional Tasks Paths
    val failurePath = s"$tasksThisRunRoot/TasksFailed.parquet"
    val successPath = s"$tasksThisRunRoot/TasksSucceeded.parquet"

    val applicationConfig = config.applicationConfig
    val loginURL = applicationConfig.loginURL
    val explorerURL = applicationConfig.explorerURL
    val securityURL = applicationConfig.securityURL
    val username = applicationConfig.username
    val password = applicationConfig.password
    val taskListName = config.taskListName

    /*------------------------------------------------*/

    implicit val httpClient = HttpClients.defaultClient

    //Run clean up operations on the task loader
    val cleanUpResponse = cleanUp(createCleanUpBody(), loginURL, explorerURL, username, password).unsafeRunSync()
    logger.info(s"Task loading cleanup successfully completed with response: ${cleanUpResponse.deleted}")

    //Get existing task list information
    val taskListInformationBeforeLoad = getInformationFromExistingTaskList[ExtraData, OutcomeData](taskListName, loginURL, explorerURL, securityURL, username, password, logger).unsafeRunSync()
    val taskListId = taskListInformationBeforeLoad.taskListId
    val privilegeSets = taskListInformationBeforeLoad.privilegeSets.map(p => (p.name, p.id)).toMap

    //Obtain tasks already loaded to list
    val tasksAlreadyInList = taskListInformationBeforeLoad.taskInfo.map(_.taskName).toSet
    logger.info(s"There are ${tasksAlreadyInList.size} tasks already in list")

    val inputTasksToLoad = spark.read.parquet(inputTasksToLoadPath).as[(ExtraData, Option[LiteGraph])]

    //Create a dataset of task name to task creation request
    val taskNameToTaskRequest = createTaskNameToTaskRequest(inputTasksToLoad, taskListId, privilegeSets)

    //Filter tasks which have already been loaded
    val taskNameToTaskRequestToLoadThisRun = taskNameToTaskRequest.filter(t => !tasksAlreadyInList.contains(t.taskName))
    logger.info(s"Tasks to load: ${taskNameToTaskRequestToLoadThisRun.count}")

    taskNameToTaskRequestToLoadThisRun.cache.show(false)
    logger.info(taskNameToTaskRequestToLoadThisRun.head)

    //Send task load requests to task loader queue
    val taskNameToWorkId: Dataset[TaskNameToTaskWorkId] =
      addTasksToLoaderQueue(taskNameToTaskRequestToLoadThisRun, loginURL, explorerURL, username, password, HttpClients.defaultClient)
    taskNameToWorkId.cache.count()

    //Wait for loading to complete by polling queue status until there are no tasks with status "Processing" or "Queued"
    val successFailures = getTaskLoadingOutcomes(loginURL, explorerURL, username, password,
      logger, FiniteDuration(5, SECONDS), 100)
    successFailures.unsafeRunSync()

    //Get information about tasks in task list after loading
    val taskListInformationAfterLoad = getInformationFromExistingTaskList[ExtraData, OutcomeData](taskListName, loginURL, explorerURL, securityURL, username, password, logger).unsafeRunSync()

    // Determine if all tasks have successfully loaded
    val tasksNotLoaded = getTasksNotLoaded(taskListInformationAfterLoad, taskNameToTaskRequest)

    //Write out the latest view of tasks which have successfully loaded to the list
    writeTasksInTaskList(taskListInformationAfterLoad, taskListName, successPath, spark)

    // Write out the latest view of tasks which have not successfully loaded
    writeTasksNotInTaskList(tasksNotLoaded, failurePath, spark)

    // Complete or fail the job as appropriate
    if ((tasksNotLoaded.size > 0)) {
      logger.error(s"There are ${tasksNotLoaded.size} which have not successfully loaded: ${tasksNotLoaded.mkString("; ")}")
      throw new IllegalStateException("Task loading not successful") // Stop job from completing successfully
    } else {
      logger.info(s"Task loading successful: ${taskListInformationAfterLoad.taskInfo.size} tasks present in task list")
    }

  }

  private def getTasksNotLoaded(taskListInformationAfterLoad: ExistingTaskListInformation,
                                taskNameToTaskRequests: Dataset[TaskNameToTaskRequest]): Set[String] = {
    import taskNameToTaskRequests.sparkSession.implicits._
    val tasksLoaded = taskListInformationAfterLoad.taskInfo.map(_.taskName).toSet
    val tasksInInput = taskNameToTaskRequests.map(_.taskName).collect.toSeq.toSet
    tasksInInput.diff(tasksLoaded)
  }

  private def writeTasksInTaskList(existingTaskListInformation: ExistingTaskListInformation,
                                   taskListName: String, outputPath: String, spark: SparkSession): Unit = {
    import spark.implicits._
    val taskNames = existingTaskListInformation.taskInfo.map(_.taskName).toSet
    val taskListId = existingTaskListInformation.taskListId.value
    taskNames.toSeq
      .toDF("taskName")
      .withColumn("taskListName", lit(taskListName))
      .withColumn("taskListId", lit(taskListId))
      .as[TaskLoadSuccess]
      .write.mode(SaveMode.Overwrite).parquet(outputPath)
  }

  private def writeTasksNotInTaskList(taskNames: Set[String], outputPath: String, spark: SparkSession): Unit = {
    import spark.implicits._
    taskNames.toSeq.toDF("taskName").write.mode("overwrite").parquet(outputPath)
  }

  /**
    * Create the task request
    */
  private def createTaskNameToTaskRequest(tasks: Dataset[(ExtraData, Option[LiteGraph])], taskListId: TaskListId,
                                          privilegeSets: Map[PrivilegeSetName, PrivilegeSetId]): Dataset[TaskNameToTaskRequest] = {

    tasks.map(taskData => {

      val taskName = taskData._1.subjectId

      val taskRequest = createTaskNewInvestigationBody(
        customerId = taskData._1.subjectId,
        customerDocumentType = taskData._1.subjectDocumentType,
        taskName = taskName,
        taskListId = taskListId,
        assignee = "User",
        liteGraph = taskData._2,
        privilegeSets = privilegeSets,
        allowedRoles = taskData._1.rolesAbleToSeeTask,
        extraData = CaseClassMapConverters.ccToMap(taskData._1)
      )
      TaskNameToTaskRequest(taskName, taskRequest)
    }
    )
  }

}

