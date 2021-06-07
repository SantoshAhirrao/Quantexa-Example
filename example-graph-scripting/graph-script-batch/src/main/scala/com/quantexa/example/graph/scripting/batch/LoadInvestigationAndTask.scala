package com.quantexa.example.graph.scripting.batch

import java.util.concurrent.TimeUnit

import com.quantexa.example.graph.scripting.GraphScriptingConfig
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders._
import com.quantexa.example.graph.scripting.stages.{LoadInvestigationStage, LoadTaskStage}
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import org.apache.log4j.Logger
import org.apache.spark.sql.{Dataset, SparkSession}
import io.circe.config.syntax._
import io.circe.generic.auto._
import com.quantexa.graph.script.spark.RestGraphScriptingDatasetExtensions._
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders._
import com.quantexa.example.graph.scripting.api.GraphScriptTaskAPI._
import com.quantexa.explorer.tasks.api.Model.TaskListId
import com.quantexa.graph.script.client.ClientResponseWrapper
import com.quantexa.graph.script.timeouts.StageTimeout
import com.quantexa.investigation.api.GraphCustodianProtocol.InitialiseGraphResponse
import org.apache.spark.sql.SaveMode.Overwrite
import java.time.LocalDateTime

import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.spark.StageContext
import com.quantexa.queue.api.Model.WorkId

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/* This batch script uses the output from LinkedRiskyAddressNetworks and runs 2 stages: LoadInvestigation and LoadTask */
object LoadInvestigationAndTask extends TypedSparkScript[GraphScriptingConfig] {

  def name = "LoadInvestigationAndTask"

  val fileDependencies = Map.empty[String, String]
  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          log: Logger,
          args: Seq[String],
          config: GraphScriptingConfig,
          etlMetricsRepository: ETLMetricsRepository): Unit = {

    import spark.implicits._

    /* CONFIGURATION */
    assert(config.loadInvestigationStage.isDefined, "Config is missing for LoadInvestigationStage.")
    assert(config.loadTaskStage.isDefined, "Config is missing for LoadTaskStage.")
    val stageTimeout: StageTimeout = StageTimeout(config.timeouts.stageTimeout.getOrElse(FiniteDuration(30L, TimeUnit.SECONDS)))
    val loadInvestigationConfig = config.loadInvestigationStage.get
    val loadTaskConfig = config.loadTaskStage.get
    val clientFactorySettings = ClientFactorySettings(config.gatewayUri,
      config.username,
      config.password,
      config.resolverConfigPath,
      stageTimeout)

    /* LOAD INPUT */
    val riskyAddressNetworks = spark.read.parquet(loadInvestigationConfig.inputPath)
      .as[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]]

    /* DEFINE STAGE 1 */
    val loadInvestigationStage: StageContext[DocumentId, EntityGraph] => FutureErrorOrWithMetrics[InitialiseGraphResponse] =
      ctx => LoadInvestigationStage(ctx.key, ctx.input, ctx.clientFactory)(ExecutionContext.global)

    /* RUN STAGE 1 */
    val loadedInvestigations: Dataset[ClientResponseWrapper[DocumentId, EntityGraph, InitialiseGraphResponse]] =
      riskyAddressNetworks.mapWithClient(clientFactorySettings, loadInvestigationStage)

    if (loadInvestigationConfig.checkpoint) {
      loadedInvestigations.write.mode(Overwrite).parquet(loadInvestigationConfig.outputPath)
    }


    /* DEFINE STAGE 2 */
    val taskListDefinitionName = "taskListDefinitionName"
    val taskListName = s"${LocalDateTime.now()}"
    val loadTaskStage: StageContext[DocumentId, InitialiseGraphResponse] => FutureErrorOrWithMetrics[WorkId] =
      ctx => {
        val taskStageInput = TaskStageInput.defaultTaskInput(
          taskListDefinitionName = taskListDefinitionName,
          taskListName = taskListName,
          singleTaskList = loadTaskConfig.taskListId.map(TaskListId(_)),
          documentId = ctx.key,
          investigationId = ctx.input.id
        )
        LoadTaskStage(taskStageInput, ctx.clientFactory)(ExecutionContext.global)
      }

    /* RUN STAGE 2 */
    val loadedTasks: Dataset[ClientResponseWrapper[DocumentId, InitialiseGraphResponse, WorkId]] =
      loadedInvestigations.mapWithClient(clientFactorySettings, loadTaskStage)

    loadedTasks.write.mode(Overwrite).parquet(loadTaskConfig.outputPath)
  }
}
