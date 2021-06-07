package com.quantexa.example.taskloading.batch

import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.taskloading.model.Model.CustomWeightedStandardisedScoreOutput
import com.quantexa.example.taskloading.batch.utils.TaskSparkUtils
import com.quantexa.example.taskloading.config.TaskLoadingConfig
import com.quantexa.example.taskloading.model.ProjectModel.{ExtraData, TaskInformationWorking}
import com.quantexa.resolver.core.EntityGraph.DocumentId
import com.quantexa.resolver.core.EntityGraphLite.LiteGraph
import com.quantexa.scriptrunner.util.DevelopmentConventions.FolderConventions.cleansedCaseClassFile
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * This job creates the tasks that will subsequently be loaded for customers selected in previous job
  * We join on the graphs output from graphs scripting and add information to extra data
  * We could also add related transactions at this stage, for example
  */
object GenerateTasksToLoad extends TypedSparkScript[TaskLoadingConfig] {

  val name = "Generate Tasks To Load"

  def fileDependencies: Map[String, String] = Map.empty

  def scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], config: TaskLoadingConfig, etlMetricsRepository: ETLMetricsRepository) = {

    import spark.implicits._

    /*--------------- CONFIG AND PATHS ---------------*/

    val scoringRoot = config.scoringRoot
    val graphScriptingRoot = config.graphScriptingRoot
    val tasksRoot = config.tasksRoot
    val customerRoot = config.customerRoot

    val tasksThisRunRoot = tasksRoot
    val scoringThisRunRoot = scoringRoot
    val graphScriptingThisRunRoot = graphScriptingRoot // In reality would point to an iteration

    // Input paths
    val selectedTaskInfoThisRunPath = s"$tasksThisRunRoot/SelectedTaskInfoThisRun.parquet"
    val scorecardOutputThisRunPath = s"${scoringThisRunRoot}/ScoreCard/CustomerScorecard"
    val consolidatedCustomerThisRunPath = cleansedCaseClassFile(customerRoot)
    val graphScriptingSuccessesThisRunPath = s"$graphScriptingThisRunRoot/Successes.parquet"

    // Output paths
    val tasksForLoadingPath = s"$tasksThisRunRoot/TasksToLoadThisRun.parquet"

    /*--------------------------------------------------*/

    val graphs = spark.read.parquet(graphScriptingSuccessesThisRunPath).as[(DocumentId, LiteGraph)]
    val scorecardOutput = spark.read.parquet(scorecardOutputThisRunPath).as[CustomWeightedStandardisedScoreOutput]
    val customer = spark.read.parquet(consolidatedCustomerThisRunPath).as[Customer]
    val taskInfo = spark.read.parquet(selectedTaskInfoThisRunPath).as[TaskInformationWorking]

    val taskScoreInfo = TaskSparkUtils.getTaskScoreInfoFromScorecard(scorecardOutput)

    val groupedScoringInformation = taskScoreInfo.groupByKey(_.subjectId).mapGroups {
      case (customerId, scoringInformationIter) => (customerId, scoringInformationIter.toSeq)
    }

    val extraDataWithoutRoles = groupedScoringInformation
      .joinWith(customer, customer("customerIdNumberString") === groupedScoringInformation("_1"), "LEFT")
      .map {
        case ((customerId, scoreInfo), customer) =>
          ExtraData(
            subjectId = customerId,
            subjectDocumentType = "customer",
            name = Option(customer).flatMap(_.fullName),
            finalScore = scoreInfo.head.subjectTotalScore.toInt,
            taskScoreInfo = scoreInfo,
            rolesAbleToSeeTask = Seq() // Added in next step
          )
      }

    val extraData = taskInfo
      .joinWith(extraDataWithoutRoles, taskInfo("customerId") === extraDataWithoutRoles("subjectId"), "LEFT")
      .map { case (tiw, extraData) => extraData.copy(rolesAbleToSeeTask = tiw.rolesAbleToSeeTask) }

    val taskData = extraData.joinWith(graphs, extraData("subjectId") === graphs("_1.value"), "LEFT").map {
      case (extraData, (_, graph)) => (extraData, Option(graph))
      case (extraData, null) => (extraData, None)
    }

    taskData.cache

    /*------------------ Validation ----------------*/

    val selectedCustomersCount = taskInfo.count
    val outputCount = taskData.count
    val selectedCustomersDistinctCount = taskInfo.map(_.customerId).distinct.count
    val outputDistinctCount = taskData.map(_._1.subjectId).distinct.count

    logger.info(s"Customers selected for task loading: ${selectedCustomersCount}")
    logger.info(s"Total output count from CreateTasksToLoad: ${outputCount}")
    assert(
      outputCount == selectedCustomersCount,
      s"Number of tasks selected for loading (${selectedCustomersCount}) not equal to number of tasks this job is outputting (${outputCount})"
    )
    assert(
      outputDistinctCount == selectedCustomersDistinctCount,
      s"Distinct customers in tasks selected for loading (${selectedCustomersDistinctCount}) not equal to distinct customers this job is outputting (${outputDistinctCount})"
    )

    /*------------------ Output -----------------*/

    taskData.write.mode(SaveMode.Overwrite).parquet(tasksForLoadingPath)

  }

}
