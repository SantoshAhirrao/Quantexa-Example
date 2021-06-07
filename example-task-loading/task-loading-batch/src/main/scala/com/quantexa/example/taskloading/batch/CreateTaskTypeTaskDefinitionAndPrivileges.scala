package com.quantexa.example.taskloading.batch

import com.quantexa.example.taskloading.config.TaskLoadingConfig
import com.quantexa.example.taskloading.privileges.ProjectPrivileges
import com.quantexa.example.taskloading.rest.HttpClients
import com.quantexa.example.taskloading.utils.TaskUtils._
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * Creates the task type and task definition, and associates privileges with the definition
  * Projects might load a task list per run (e.g. every week) in production
  * Task types and definitions can be shared across task lists, which aids long term maintenance
  * For most projects this is therefore likely to be a 'run once' job
  */
object CreateTaskTypeTaskDefinitionAndPrivileges extends TypedSparkScript[TaskLoadingConfig] {

  def name: String = "Create Task Type, Definition And Privileges"

  def fileDependencies: Map[String, String] = Map.empty

  def scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], config: TaskLoadingConfig, etlMetricsRepository: ETLMetricsRepository) = {

    import spark.implicits._

    val applicationConfig = config.applicationConfig
    val loginURL = applicationConfig.loginURL
    val explorerURL = applicationConfig.explorerURL
    val username = applicationConfig.username
    val password = applicationConfig.password

    val taskTypeName = "ProjectTaskType"
    val taskListDefinitionName = "ProjectTaskListDefinition"

    val taskListDefinitionInfoPath = s"${config.tasksRoot}/taskListDefinitionInfoPath.parquet"

    implicit val httpClient = HttpClients.defaultClient

    val createTaskTypeTaskDefinitionAndPrivilegesIO =
      createTaskTypeTaskDefinitionAndPrivileges(taskTypeName, taskListDefinitionName, ProjectPrivileges.projectPrivileges,
        loginURL, explorerURL, username, password, logger)

    val taskListDefinitionInfo = createTaskTypeTaskDefinitionAndPrivilegesIO.unsafeRunSync()
    Seq(taskListDefinitionInfo).toDS.write.mode(SaveMode.Overwrite).parquet(taskListDefinitionInfoPath)
  }

}

