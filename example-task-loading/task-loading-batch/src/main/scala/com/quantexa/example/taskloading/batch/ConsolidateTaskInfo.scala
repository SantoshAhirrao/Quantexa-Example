package com.quantexa.example.taskloading.batch

import com.quantexa.example.taskloading.config.TaskLoadingConfig
import com.quantexa.example.taskloading.model.ProjectModel.{TaskInformationFinal, TaskInformationWorking, TaskLoadSuccess}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

object ConsolidateTaskInfo extends TypedSparkScript[TaskLoadingConfig] {

  val name = "Consolidate Task Info"

  def fileDependencies: Map[String, String] = Map.empty

  def scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], config: TaskLoadingConfig, etlMetricsRepository: ETLMetricsRepository) = {

    import spark.implicits._

    /*--------------- CONFIG AND PATHS -----------------*/

    val tasksRoot = config.tasksRoot

    val tasksThisRunRoot = tasksRoot
    val taskPreviousRunRoot: Option[String] = None // In reality, would be populated or set to None if initial run

    val tasksThisRunPath = s"$tasksThisRunRoot/SelectedTaskInfoThisRun.parquet" // From SelectCustomersToRaiseTasksFor

    val consolidatedTasksPathThisRun = s"$tasksThisRunRoot/consolidated/Tasks.parquet" // New consolidated tasks dataset
    val consolidatedTasksPathPreviousRun: Option[String] = taskPreviousRunRoot.map(previousRoot => s"${previousRoot}/consolidated/Tasks.parquet")

    val taskSuccessPath = s"$tasksThisRunRoot/TasksSucceeded.parquet"

    /*--------------------------------------------------*/

    val tasksThisRun = spark.read.parquet(tasksThisRunPath).as[TaskInformationWorking]
    logger.info(s"tasksThisRun count: ${tasksThisRun.count}")

    val taskLoadSuccess = spark.read.parquet(taskSuccessPath).as[TaskLoadSuccess]
    logger.info(s"taskLoadSuccess count: ${taskLoadSuccess.count}")

    val consolidatedTaskInformationPreviousRuns = consolidatedTasksPathPreviousRun match {
      case Some(path) => {
        logger.info(s"Reading consolidated tasks from previous run at $path")
        spark.read.parquet(path).as[TaskInformationFinal]
      }
      case None => {
        logger.info(s"No consolidated tasks dataset from previous run. Creating empty dataset.")
        spark.emptyDataset[TaskInformationFinal]
      }
    }
    logger.info(s"consolidatedTaskInformationPreviousRuns count: ${consolidatedTaskInformationPreviousRuns.count}")

    // Join successes with task information so we only consolidate tasks which have definitely been loaded
    val tasksThisRunWithTaskList = tasksThisRun.joinWith(
      taskLoadSuccess, tasksThisRun("customerId") === taskLoadSuccess("taskName"), "INNER")
      .map { case (taskInfoWorking, taskLoadSuccess) => {
        taskInfoWorking.toTaskInformationFinal(taskListId = taskLoadSuccess.taskListId, taskListName = taskLoadSuccess.taskListName)
      }
      }

    tasksThisRunWithTaskList.cache()

    // Check we haven't accidentally got duplicates
    assert(tasksThisRunWithTaskList.map(_.customerId).distinct.count == tasksThisRunWithTaskList.count, "tasksThisRunWithTaskList is not distinct by customerId")

    logger.info(s"tasksThisRunWithTaskList count: ${tasksThisRunWithTaskList.count}")

    val consolidated = consolidatedTaskInformationPreviousRuns.union(tasksThisRunWithTaskList)
    logger.info(s"consolidated count: ${consolidated.count}")
    consolidated.write.mode("overwrite").parquet(consolidatedTasksPathThisRun)
  }

}
