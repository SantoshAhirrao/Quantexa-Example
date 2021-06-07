package com.quantexa.example.graph.scripting.batch.rest

import com.quantexa.example.graph.scripting.GraphScriptBulkLoaderConfig
import com.quantexa.resolver.core.EntityGraph.DocumentId
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import org.apache.log4j.Logger
import org.apache.spark.sql.{Dataset, SparkSession}
import io.circe.generic.auto._
import io.circe.config.syntax._
import io.circe.generic.auto.exportDecoder
import com.quantexa.example.graph.scripting.batch.encoders.CirceEncoders._
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders._
import com.quantexa.graph.script.utils.{BulkRequestHandler, RestScalaClient}
import com.quantexa.graph.script.utils.BulkRequestHandler.BulkRequestConfig
import com.quantexa.graph.script.utils.BulkRequesterAPI.{BulkRequesterFail, BulkRequesterResponseWithTiming, BulkRequesterSuccess}
import com.quantexa.graph.script.utils.GenericDerivation._
import com.quantexa.resolver.core.EntityGraphLite.LiteGraph

object BulkOneHop extends TypedSparkScript[GraphScriptBulkLoaderConfig]{
  val name = "GraphScriptBulkRequest"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], config: GraphScriptBulkLoaderConfig, etlMetricsRepository: ETLMetricsRepository): Unit = {

    import spark.implicits._

    val frontendUrl = config.url.getOrElse("http://localhost:8080")
    val loginUrl = s"$frontendUrl/api/authenticate"
    val postUrl = s"$frontendUrl/api/app-graph-script/graph-scripting/one-hop-customer/"
    val getUrl = s"$frontendUrl/api/app-graph-script/graph-scripting/one-hop-customer-check/"

    val oneHopConfig = config.oneHopStage.getOrElse(throw new IllegalStateException("Could not find config for OneHopStage"))

    //TODO: Replace the .map with .as
    val docIdDS = spark.read.parquet(oneHopConfig.inputPath).map(x => new DocumentId(x.getString(0), x.getString(1)))

    val graphDS: Dataset[BulkRequesterResponseWithTiming[LiteGraph]] = BulkRequestHandler.submit(
      docIdDS,
      BulkRequestConfig(loginUrl, postUrl, getUrl, config.username, config.password),
      RestScalaClient.httpClient(config.sslEnabled))
      .cache()

    val completed = graphDS.filter(_.bulkRequesterResponse.isInstanceOf[BulkRequesterSuccess[LiteGraph]])
      .map(_.bulkRequesterResponse.asInstanceOf[BulkRequesterSuccess[LiteGraph]])
    val failed = graphDS.filter(_.bulkRequesterResponse.isInstanceOf[BulkRequesterFail])
      .map(_.bulkRequesterResponse.asInstanceOf[BulkRequesterFail])

    logger.info(s"graphDS Count: ${graphDS.count()}")
    logger.info(s"completed Count: ${completed.count()}")
    logger.info(s"failed Count: ${failed.count()}")

    logger.info(s"Saving outputGraphs to: ${oneHopConfig.outputPath}")
    graphDS.write.mode("overwrite").format("parquet").save(oneHopConfig.outputPath)

    val successPath = replaceFilename(oneHopConfig.outputPath, "Successes.parquet")
    val failurePath = replaceFilename(oneHopConfig.outputPath, "Failures.parquet")

    val completedTransformed = completed.map(comp =>
      (new DocumentId("customer", extractDocumentIdValueFromSuccessKey(comp.successKey)), comp.output)
    )

    logger.info(s"Saving successes to: $successPath")
    completedTransformed.write.mode("overwrite").parquet(successPath)

    failed.write.mode("overwrite").parquet(failurePath)
  }

  private def replaceFilename(fullPath: String, newFileName: String): String = {
    fullPath.split("/").dropRight(1).mkString("/") + s"/$newFileName"
  }

  private def extractDocumentIdValueFromSuccessKey(successKey: String): String = {
    successKey.split("_")(1)
  }

}
