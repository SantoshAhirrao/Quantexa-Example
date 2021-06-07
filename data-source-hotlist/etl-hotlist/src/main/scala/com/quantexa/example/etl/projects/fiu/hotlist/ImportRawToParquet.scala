package com.quantexa.example.etl.projects.fiu.hotlist

import com.quantexa.etl.validation.schema.ReaderWithSchemaValidation
import com.quantexa.example.etl.projects.fiu.ETLConfig
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils.{readCSV, convertAndValidateCSV}
import com.quantexa.example.model.fiu.hotlist.HotlistRawModel.HotlistRaw
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, SparkSession}

/***
  * QuantexaSparkScript used to convert the following CSV files to Parquet Files for the FIU Smoke Test Data:
  * Input: raw/hotlist.csv
  * Output: raw/parquet/hotlist.parquet
  *
  * Stage 1
  * At this stage we want to change the format of the raw data so that we store it in parquet columnar format.
  * There should be no changes to the data structure, or any parsing, just a basic extract and load.
  */
object ImportRawToParquet extends TypedSparkScript[ETLConfig] {
  val name = "ImportRawToParquet - Hotlist"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: ETLConfig,
          etlMetricsRepository: ETLMetricsRepository): Unit = {

    import spark.implicits._

    val destinationPath = projectConfig.hotlist.parquetRoot
    val hotlistPath = projectConfig.hotlist.inputFiles.hotlist

    val hotlistReader = new ReaderWithSchemaValidation[HotlistRaw] {
      def df(path: String): DataFrame = readCSV(path, spark)
    }

    // read hotlist CSV file, validate schema, convert to dataset and then write out typed parquet file
    convertAndValidateCSV(hotlistReader, hotlistPath, destinationPath, spark, logger)
  }
}
