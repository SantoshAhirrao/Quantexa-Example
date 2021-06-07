package com.quantexa.example.etl.projects.fiu.transaction

import com.quantexa.example.etl.projects.fiu.ETLConfig
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.util.incremental.MetaDataRepositoryImpl._
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import org.apache.log4j.Logger
import io.circe.generic.auto.exportDecoder
import org.apache.spark.sql.SparkSession

/***
  * GenerateMetadataRow retrieves the run Id from the config if it has been specified and adds a new row to metadata with the specified run id
  * If it returns an empty string, it generates a run Id using time and adds a new row to metadata with that id
  */
object GenerateMetadataRow extends TypedSparkScript[ETLConfig] {
  val name = "Generate Metadata Row"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: ETLConfig, etlMetricsRepository: ETLMetricsRepository): Unit = {
    if (args.nonEmpty) {
      logger.warn(args.length + " arguments were passed to the script and are being ignored")
    }

    val currentTime = new java.sql.Timestamp(System.currentTimeMillis()).toString

    val newMetadataRunId = if(projectConfig.transaction.runId.isDefined){
      val runId = projectConfig.transaction.runId.get
      MetadataRunId(runId, currentTime, hasher(runId, currentTime))
    } else {
      val runId = currentTime.replace(" ", "_").replaceAll("[.:]", "-")
      MetadataRunId(runId, currentTime, hasher(runId, currentTime))
    }

    generateMetadataRow(spark, projectConfig.transaction.metadataPath, newMetadataRunId, logger, name)
  }
}
