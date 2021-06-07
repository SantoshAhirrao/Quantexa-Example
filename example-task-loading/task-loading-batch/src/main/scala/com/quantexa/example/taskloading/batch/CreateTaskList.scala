package com.quantexa.example.taskloading.batch

import com.quantexa.example.taskloading.config.TaskLoadingConfig
import com.quantexa.example.taskloading.model.Model.TaskListDefinitionInfo
import com.quantexa.example.taskloading.rest.HttpClients
import com.quantexa.example.taskloading.utils.TaskUtils._
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

/**
  * Create a new task list, using existing task type and task list definition
  */
object CreateTaskList extends TypedSparkScript[TaskLoadingConfig] {

  def name: String = "Create Task List"

  def fileDependencies: Map[String, String] = Map.empty

  def scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], config: TaskLoadingConfig, etlMetricsRepository: ETLMetricsRepository) = {

    import spark.implicits._

    val applicationConfig = config.applicationConfig
    val loginURL = applicationConfig.loginURL
    val explorerURL = applicationConfig.explorerURL
    val username = applicationConfig.username
    val password = applicationConfig.password

    val taskListName = config.taskListName

    val taskListDefinitionInfoPath = s"${config.tasksRoot}/taskListDefinitionInfoPath.parquet"

    implicit val httpClient = HttpClients.defaultClient

    val taskListDefinitionInfoDS = spark.read.parquet(taskListDefinitionInfoPath).as[TaskListDefinitionInfo]

    if (taskListDefinitionInfoDS.count != 1) throw new IllegalStateException(s"TaskListDefinitionInfo should contain 1 row (actual number of rows found: ${taskListDefinitionInfoDS.count})")
    val taskListDefinitionInfo = taskListDefinitionInfoDS.collect()(0)

    createNewTaskListCheckingForExisting(taskListName, taskListDefinitionInfo, loginURL, explorerURL, username, password, logger).unsafeRunSync()
  }

}
