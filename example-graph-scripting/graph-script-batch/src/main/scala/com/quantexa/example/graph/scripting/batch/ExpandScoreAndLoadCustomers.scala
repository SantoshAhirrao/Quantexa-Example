package com.quantexa.example.graph.scripting.batch

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

import com.quantexa.example.graph.scripting.GraphScriptingConfig
import com.quantexa.example.graph.scripting.api.GraphScriptTaskAPI.TaskStageInput
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders._
import com.quantexa.example.graph.scripting.stages._
import com.quantexa.explorer.tasks.api.Model.TaskListId
import com.quantexa.graph.script.client.ClientResponseWrapper
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.spark.RestGraphScriptingDatasetExtensions._
import com.quantexa.graph.script.spark.StageContext
import com.quantexa.graph.script.timeouts.StageTimeout
import com.quantexa.graph.script.utils.EntityGraphWithScore
import com.quantexa.graph.script.utils.GraphScoringUtils.sumScoreOutputs
import com.quantexa.graph.script.utils.StageUtils.composeGraphScriptStage
import com.quantexa.investigation.api.GraphCustodianProtocol.InitialiseGraphResponse
import com.quantexa.queue.api.Model.WorkId
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.config.syntax._
import io.circe.generic.auto._
import org.apache.log4j.Logger
import org.apache.spark.sql.SaveMode.Overwrite
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/* This script runs 4 stages: CustomerOneHopExpansionStage, ScoreCustomerOneHopNetworkStage,
 * LoadInvestigationStage and LoadTaskStage. The real time version is ExpandScoreAndLoadScript */
object ExpandScoreAndLoadCustomers extends TypedSparkScript[GraphScriptingConfig] {

  def name = "ExpandScoreAndLoadCustomers"

  val fileDependencies = Map.empty[String, String]
  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          log: Logger,
          args: Seq[String],
          config: GraphScriptingConfig,
          etlMetricsRepository: ETLMetricsRepository): Unit = {


    import spark.implicits._

    /* CONFIGURATION */
    assert(config.oneHopStage.isDefined, "Config is missing for CustomerOneHopExpansionStage.")
    assert(config.scoreCustomerOneHopNetworkStage.isDefined, "Config is missing for ScoreCustomerOneHopNetworkStage.")
    assert(config.loadInvestigationStage.isDefined, "Config is missing for LoadInvestigationStage.")
    assert(config.loadTaskStage.isDefined, "Config is missing for LoadTaskStage.")
    val stageTimeout: StageTimeout = StageTimeout(config.timeouts.stageTimeout.getOrElse(FiniteDuration(30L, TimeUnit.SECONDS)))
    val oneHopConfig = config.oneHopStage.get
    val scoreOneHopNetworkConfig = config.scoreCustomerOneHopNetworkStage.get
    val loadInvestigationConfig = config.loadInvestigationStage.get
    val loadTaskConfig = config.loadTaskStage.get
    val clientFactorySettings = ClientFactorySettings(config.gatewayUri,
      config.username,
      config.password,
      config.resolverConfigPath,
      stageTimeout)

    /* LOAD INPUT */
    val rawInputIds = spark.read.parquet(oneHopConfig.inputPath)
      .map(x => new DocumentId(x.getString(0), x.getString(1)))
      .map(docId => (docId, docId))

    /* DEFINE STAGE 1*/
    val customerOneHopExpansionStage: StageContext[DocumentId, DocumentId] => FutureErrorOrWithMetrics[EntityGraph] =
      ctx =>
        CustomerOneHopExpansionStage(ctx.input, Some(config), ctx.clientFactory)(ExecutionContext.global)

    /* RUN STAGE 1 */
    val entityGraphs = rawInputIds.mapWithClient(clientFactorySettings, customerOneHopExpansionStage)

    /* When running multiple stages, recommended to add optional intermediary write steps */
    if (oneHopConfig.checkpoint) {
      entityGraphs.write.mode(Overwrite).parquet(oneHopConfig.outputPath)
    }

    /* DEFINE STAGE 2 */
    val scoreCustomerOneHopStage: StageContext[DocumentId, EntityGraph] => FutureErrorOrWithMetrics[EntityGraphWithScore] =
      ctx =>
        ScoreCustomerOneHopNetworkStage(ctx.input, ctx.clientFactory)(ExecutionContext.global)

    /* RUN STAGE 2 */
    val resultScoredGraphs: Dataset[ClientResponseWrapper[DocumentId, EntityGraph, EntityGraphWithScore]] = entityGraphs
      .mapWithClient(clientFactorySettings, scoreCustomerOneHopStage)

    /* When running multiple stages, recommended to add optional intermediary write steps */
    if (scoreOneHopNetworkConfig.checkpoint) {
      resultScoredGraphs.write.mode(Overwrite).parquet(scoreOneHopNetworkConfig.outputPath)
    }

    /*
      CUSTOM LOGIC

      Wherever possible, logic should be performed in the stages/steps rather than at the Spark-submit level.
      However, in some situations it is for more convenient to use Spark. This is equivalent to FilterHighScoresStage
      */
    val Threshold = 100F
    val networksScoringOverThreshold: Dataset[(DocumentId, EntityGraphWithScore)] = composeGraphScriptStage(resultScoredGraphs)
      .filter { keyAndEntityGraphWithScore =>
        val (_, entityGraphWithScore) = keyAndEntityGraphWithScore
        sumScoreOutputs(entityGraphWithScore.scoreOutputs) > Threshold
      }

    /* DEFINE STAGE 3 */
    val loadInvestigationStage: StageContext[DocumentId, EntityGraphWithScore] => FutureErrorOrWithMetrics[InitialiseGraphResponse] =
      ctx =>
        LoadInvestigationStage.apply(ctx.key, ctx.input.entityGraph, ctx.clientFactory)(ExecutionContext.global)

    /* RUN STAGE 3 */
    val loadedInvestigations = networksScoringOverThreshold.mapWithClient(clientFactorySettings, loadInvestigationStage)

    loadedInvestigations.write.mode(Overwrite).parquet(loadInvestigationConfig.outputPath)


    /* DEFINE STAGE 4 */
    val taskListDefinitionName = "taskListDefinitionName"
    val taskListName = s"${LocalDateTime.now}"
    val loadTaskStage: StageContext[DocumentId, InitialiseGraphResponse] => FutureErrorOrWithMetrics[WorkId] =
      ctx=> {
        val taskStageInput = TaskStageInput.defaultTaskInput(
          taskListDefinitionName = taskListDefinitionName,
          taskListName = taskListName,
          singleTaskList = loadTaskConfig.taskListId.map(TaskListId(_)),
          documentId = ctx.key,
          investigationId = ctx.input.id
        )
        LoadTaskStage(taskStageInput, ctx.clientFactory)(ExecutionContext.global)
      }

    /* RUN STAGE 4 */
    val loadedTasks: Dataset[ClientResponseWrapper[DocumentId, InitialiseGraphResponse, WorkId]] =
      loadedInvestigations.mapWithClient(clientFactorySettings, loadTaskStage)

    loadedTasks.write.mode(Overwrite).parquet(loadTaskConfig.outputPath)
  }
}
