package com.quantexa.example.etl.projects.fiu.highriskcountrycodes

import com.quantexa.etl.validation.schema.ReaderWithSchemaValidation
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils._
import com.quantexa.example.etl.projects.fiu.ETLConfig
import com.quantexa.example.model.fiu.lookupdatamodels.HighRiskCountryCode
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, SparkSession}

object ImportRawToParquet extends TypedSparkScript[ETLConfig] {
  val name = "High Risk Country Codes"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          logger: Logger,
          args: Seq[String],
          etlConfig: ETLConfig,
          etlMetricsRepository: ETLMetricsRepository): Unit = {
    import spark.implicits._

    val destinationPath = etlConfig
      .scoringLookups
      .getOrElse(throw new IllegalStateException("No destination path found in config. Please specify a value for 'scoringLookups.outputRoot'."))
      .outputRoot

    val inputPath = etlConfig
      .scoringLookups
      .getOrElse(throw new IllegalStateException("No input path found in config. Please specify a value for 'scoringLookups.highRiskCountryCodes'."))
      .highRiskCountryCodes

    val reader = new ReaderWithSchemaValidation[HighRiskCountryCode] {
      def df(path: String): DataFrame = readCSV(path, spark)
    }

    // read lookup file, validate schema, convert to dataset and then write out typed parquet file
    convertAndValidateCSV(reader, inputPath, destinationPath, spark, logger)
  }
}
