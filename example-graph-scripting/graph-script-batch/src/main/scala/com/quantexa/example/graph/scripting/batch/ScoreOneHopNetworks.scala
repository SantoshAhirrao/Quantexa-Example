package com.quantexa.example.graph.scripting.batch

import java.util.concurrent.TimeUnit

import com.quantexa.example.graph.scripting.GraphScriptingConfig
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders.{clientResponseEntityGraphEncoder, clientResponseEntityGraphWithScoreEncoder, documentIdWithEntityGraphEncoder}
import com.quantexa.example.graph.scripting.stages.ScoreCustomerOneHopNetworkStage
import com.quantexa.graph.script.client.ClientResponseWrapper
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.spark.RestGraphScriptingDatasetExtensions._
import com.quantexa.graph.script.spark.StageContext
import com.quantexa.graph.script.timeouts.StageTimeout
import com.quantexa.graph.script.utils.EntityGraphWithScore
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

/* This batch script uses the output from OneHopNetworks and scores the graph */
object ScoreOneHopNetworks extends TypedSparkScript[GraphScriptingConfig] {

  def name = "ScoreOneHopNetworks"

  val fileDependencies = Map.empty[String, String]
  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          logger: Logger,
          args: Seq[String],
          config: GraphScriptingConfig,
          etlMetricsRepository: ETLMetricsRepository): Unit = {

    import spark.implicits._

    /* CONFIGURATION */
    assert(config.scoreCustomerOneHopNetworkStage.isDefined, "Config is missing for ScoreCustomerOneHopNetworkStage.")
    val scoreOneHopNetworksConfig = config.scoreCustomerOneHopNetworkStage.get
    val stageTimeout = StageTimeout(config.timeouts.stageTimeout.getOrElse(FiniteDuration(30L, TimeUnit.SECONDS)))
    val clientFactorySettings = ClientFactorySettings(config.gatewayUri,
      config.username,
      config.password,
      config.resolverConfigPath,
      stageTimeout)

    /* LOAD INPUT */
    val outputFromOneHopStage = spark.read.parquet(scoreOneHopNetworksConfig.inputPath).as[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]]

    /* DEFINE STAGE */
    val scoreCustomerOneHopStage: StageContext[DocumentId, EntityGraph] => FutureErrorOrWithMetrics[EntityGraphWithScore] =
      ctx => ScoreCustomerOneHopNetworkStage(ctx.input, ctx.clientFactory)(ExecutionContext.global)

    /* RUN STAGE */
    val resultScoredGraphs: Dataset[ClientResponseWrapper[DocumentId, EntityGraph, EntityGraphWithScore]] =
      outputFromOneHopStage.mapWithClient(clientFactorySettings, scoreCustomerOneHopStage)

    resultScoredGraphs.write.mode(Overwrite).parquet(scoreOneHopNetworksConfig.outputPath)
  }
}
