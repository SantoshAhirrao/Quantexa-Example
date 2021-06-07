package com.quantexa.example.etl.projects.fiu.transaction

import com.quantexa.etl.validation.schema.ReaderWithSchemaValidation
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils._
import com.quantexa.example.etl.projects.fiu.{DocumentConfig, TransactionInputFile}
import com.quantexa.example.model.fiu.customer.CustomerRawModel.AccToCusRaw
import com.quantexa.example.model.fiu.transaction.TransactionRawModel.TransactionRaw
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScriptIncremental}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import io.circe.generic.auto._
import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, SparkSession}

/***
  * QuantexaSparkScript used to convert the following CSV files to Parquet Files for the FIU Smoke Test Data
  * Input: raw/transactions.csv
  * Output: raw/parquet/transactions.parquet
  *
  * Stage 1
  * At this stage we want to change the format of the raw data so that we store it in parquet columnar format.
  * There should be no changes to the data structure, or any parsing, just a basic extract and load.
  */
object ImportRawToParquet extends TypedSparkScriptIncremental[DocumentConfig[TransactionInputFile]] {
  val name = "ImportRawToParquet - Transaction"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]


  def run(spark: SparkSession,
          logger: Logger,
          args: Seq[String],
          projectConfig: DocumentConfig[TransactionInputFile],
          etlMetricsRepository: ETLMetricsRepository,
          metadataRunId: MetadataRunId): Unit = {

    import spark.implicits._

    val destinationPath = projectConfig.parquetRoot
    val transactionPath = projectConfig.inputFiles.transaction
    val accToCusPath = projectConfig.inputFiles.acc_to_cus

    val transactionReader = new ReaderWithSchemaValidation[TransactionRaw] {
      def df(path: String): DataFrame = readCSV(path, spark)
    }
    val accToCusReader = new ReaderWithSchemaValidation[AccToCusRaw] {
      def df(path: String): DataFrame = readCSV(path, spark)
    }

    // read transaction CSV file, validate schema, convert to dataset and then write out typed parquet file
    convertAndValidateCSV(transactionReader, transactionPath, destinationPath, spark, logger)
    // read account CSV file, validate schema, convert to dataset and then write out typed parquet file
    convertAndValidateCSV(accToCusReader, accToCusPath, destinationPath, spark, logger)
  }
}
