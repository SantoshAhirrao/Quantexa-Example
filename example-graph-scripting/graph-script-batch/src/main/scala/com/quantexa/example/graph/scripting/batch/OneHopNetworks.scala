package com.quantexa.example.graph.scripting.batch

import java.util.concurrent.TimeUnit

import com.quantexa.example.graph.scripting.GraphScriptingConfig
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders._
import com.quantexa.example.graph.scripting.stages.CustomerOneHopExpansionStage
import com.quantexa.graph.script.client.ClientResponseWrapper
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.spark.RestGraphScriptingDatasetExtensions._
import com.quantexa.graph.script.spark.StageContext
import com.quantexa.graph.script.timeouts.StageTimeout
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.config.syntax._
import io.circe.generic.auto._
import org.apache.log4j.Logger
import org.apache.spark.sql.SaveMode.Overwrite
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

//This script takes a set of documents and expands them into entity graphs.
object OneHopNetworks extends TypedSparkScript[GraphScriptingConfig] {

  def name = "OneHopNetworks"

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
    val oneHopConfig = config.oneHopStage.get
    val stageTimeout: StageTimeout = StageTimeout(config.timeouts.stageTimeout.getOrElse(FiniteDuration(30L, TimeUnit.SECONDS)))
    val clientFactorySettings = ClientFactorySettings(config.gatewayUri,
      config.username,
      config.password,
      config.resolverConfigPath,
      stageTimeout)

    /* LOAD INPUT */
    val rawInputIds = spark.read.parquet(oneHopConfig.inputPath)
      .as[DocumentId]
      .map(docId => (docId, docId))

    /* DEFINE STAGE */
    val oneHopExpansionStage: StageContext[DocumentId, DocumentId] => FutureErrorOrWithMetrics[EntityGraph] =
      ctx => CustomerOneHopExpansionStage(ctx.input, Some(config), ctx.clientFactory)

    /* RUN STAGE */
    val entityGraphs: Dataset[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]] =
      rawInputIds.mapWithClient(clientFactorySettings, oneHopExpansionStage)

    entityGraphs.write.mode(Overwrite).parquet(oneHopConfig.outputPath)
  }
}
