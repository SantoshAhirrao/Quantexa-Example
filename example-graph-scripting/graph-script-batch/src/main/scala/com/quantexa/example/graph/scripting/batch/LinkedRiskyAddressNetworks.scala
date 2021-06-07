package com.quantexa.example.graph.scripting.batch

import java.util.concurrent.TimeUnit

import com.quantexa.example.graph.scripting.GraphScriptingConfig
import com.quantexa.example.graph.scripting.stages.{CustomerTwoHopIndividualsOnlyStage, LinkedRiskyAddressesStage}
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import org.apache.log4j.Logger
import org.apache.spark.sql.Dataset
import io.circe.config.syntax._
import io.circe.generic.auto._
import org.apache.spark.sql.SparkSession
import com.quantexa.graph.script.spark.RestGraphScriptingDatasetExtensions._
import org.apache.spark.sql.SaveMode.Overwrite
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders._
import com.quantexa.graph.script.client.ClientResponseWrapper
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.spark.StageContext
import com.quantexa.graph.script.timeouts.StageTimeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/* This script runs 2 stages: CustomerTwoHopIndividualsOnlyStage and LinkedRiskyAddressesStage. */
object LinkedRiskyAddressNetworks extends TypedSparkScript[GraphScriptingConfig] {

  def name = "LinkedRiskyAddressNetworks"

  val fileDependencies = Map.empty[String, String]
  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          log: Logger,
          args: Seq[String],
          config: GraphScriptingConfig,
          etlMetricsRepository: ETLMetricsRepository): Unit = {

    import spark.implicits._

    /* CONFIGURATION */
    assert(config.twoHopIndividualsOnlyStage.isDefined, "Config is missing for CustomerTwoHopIndividualsOnlyStage.")
    assert(config.linkedRiskyAddressesStage.isDefined, "Config is missing for LinkedRiskyAddressesStage.")
    val stageTimeout: StageTimeout = StageTimeout(config.timeouts.stageTimeout.getOrElse(FiniteDuration(30L, TimeUnit.SECONDS)))
    val twoHopConfig = config.twoHopIndividualsOnlyStage.get
    val linkedRiskyAddressesConfig = config.linkedRiskyAddressesStage.get
    val clientFactorySettings = ClientFactorySettings(config.gatewayUri,
      config.username,
      config.password,
      config.resolverConfigPath,
      stageTimeout)

    /* LOAD INPUT */
    val rawInputIds = spark.read.parquet(twoHopConfig.inputPath)
      .as[DocumentId]
      .map(docId => (docId, docId))

    /* DEFINE STAGE 1 */
    val customerTwoHopIndividualsStage: StageContext[DocumentId, DocumentId] => FutureErrorOrWithMetrics[EntityGraph] =
      ctx =>
        CustomerTwoHopIndividualsOnlyStage(ctx.input, ctx.clientFactory)(ExecutionContext.global)

    /* RUN STAGE 1 */
    val customerTwoHopNetwork: Dataset[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]] =
      rawInputIds.mapWithClient(clientFactorySettings, customerTwoHopIndividualsStage)

    customerTwoHopNetwork.write.mode(Overwrite).parquet(twoHopConfig.outputPath)

    /* DEFINE STAGE 2 */
    val linkedRiskyAddressesStage: StageContext[DocumentId, EntityGraph] => FutureErrorOrWithMetrics[EntityGraph] =
      ctx =>
        LinkedRiskyAddressesStage(ctx.key, ctx.input, ctx.resolverConfigurationForMerge, ctx.clientFactory)(ExecutionContext.global)

    /* RUN STAGE 2 */
    val entityGraphs: Dataset[ClientResponseWrapper[DocumentId, EntityGraph, EntityGraph]] =
      customerTwoHopNetwork.mapWithClient(clientFactorySettings, linkedRiskyAddressesStage)

    entityGraphs.write.mode(Overwrite).parquet(linkedRiskyAddressesConfig.outputPath)
  }
}
