package com.quantexa.example.graph.scripting.batch.metrics

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.quantexa.etl.core.elastic.ElasticLoaderFullSettings
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders.clientResponseEntityGraphEncoder
import com.quantexa.graph.script.client.ClientResponseWrapper
import com.quantexa.graph.script.metrics.MetricsDashboardUtils
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import org.elasticsearch.spark.rdd.EsSpark

object UploadResolverMetricsToElastic extends TypedSparkScript[ElasticLoaderFullSettings]()(ElasticLoaderFullSettingsDecoder.decoder) {
  def name = "UploadResolverMetricsToElastic"
  val fileDependencies = Map.empty[String, String]
  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          log: Logger,
          args: Seq[String],
          config: ElasticLoaderFullSettings,
          etlMetricsRepository: ETLMetricsRepository): Unit = {

    import spark.implicits._

    // The name of the index to upload to, e.g. "graph-script-shell-stage-1"
    val indexName = config.index.name
    // The path to the output parquet from OneHopNetworks
    val inputPath = config.hdfsRoot

    //For resolver metrics, the third type of ClientResponseWrapper must be transformed to be EntityGraph
    val outputFromOneHopNetworks = spark.read.parquet(inputPath).
      as[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]](clientResponseEntityGraphEncoder)

    @transient lazy val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

    val performanceMetrics = MetricsDashboardUtils.createViewablePerformanceMetrics(outputFromOneHopNetworks)

    val entityPerformanceMetrics = performanceMetrics.entityPerformanceMetrics.map(x => mapper.writeValueAsString(x)).rdd
    val documentPerformanceMetrics = performanceMetrics.documentPerformanceMetrics.map(x => mapper.writeValueAsString(x)).rdd
    val stageTimePerformanceMetrics = performanceMetrics.stageTimeMetrics.map(x => mapper.writeValueAsString(x)).rdd

    val linkingMetrics = MetricsDashboardUtils.createViewableLinkingMetrics(outputFromOneHopNetworks)

    val graphMetrics = linkingMetrics.map(_.graphMetrics).map(x => mapper.writeValueAsString(x)).rdd
    val entityConnectionsMetrics = linkingMetrics.flatMap(_.entityMetrics).map(x => mapper.writeValueAsString(x)).rdd

    //TODO: Before v0.10.X, you didn't need to specify index type. Now it has to be "search" or "resolver", so this is a bit of a hack because we have neither.
    val loaderSettings = config.sparkLoaderSettings("search")

    EsSpark.saveJsonToEs(entityPerformanceMetrics, Map("es.resource" -> s"$indexName/default") ++ loaderSettings)
    EsSpark.saveJsonToEs(documentPerformanceMetrics, Map("es.resource" -> s"$indexName/default") ++ loaderSettings)
    EsSpark.saveJsonToEs(stageTimePerformanceMetrics, Map("es.resource" -> s"$indexName/default") ++ loaderSettings)
    EsSpark.saveJsonToEs(graphMetrics, Map("es.resource" -> s"$indexName/default") ++ loaderSettings)
    EsSpark.saveJsonToEs(entityConnectionsMetrics, Map("es.resource" -> s"$indexName/default") ++ loaderSettings)
  }
}

object ElasticLoaderFullSettingsDecoder{
  implicit val customConfig: Configuration = Configuration.default.withDefaults
  implicit val decoder = io.circe.generic.extras.auto.exportDecoder[ElasticLoaderFullSettings].instance
}