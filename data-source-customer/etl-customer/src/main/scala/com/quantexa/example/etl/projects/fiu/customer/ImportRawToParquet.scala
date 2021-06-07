package com.quantexa.example.etl.projects.fiu.customer

import com.quantexa.etl.validation.schema.ReaderWithSchemaValidation
import com.quantexa.example.etl.projects.fiu.{CustomerInputFiles, DocumentConfig}
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils.{readCSV, convertAndValidateCSV}
import com.quantexa.example.model.fiu.customer.CustomerRawModel.{AccToCusRaw, AccountRaw, CustomerRaw}
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScriptIncremental}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, SparkSession}

/***
  * QuantexaSparkScript used to convert the following CSV files to Parquet Files for the FIU Smoke Test Data:
  * Input: raw/customer.csv, raw/acc_to_cus.csv, raw/account.csv
  * Output: raw/parquet/customer.parquet, raw/parquet/acc_to_cus.parquet, raw/parquet/account.parquet
  *
  * At this stage we want to change the format of the raw data so that we store it in parquet columnar format.
  * There should be no changes to the data structure, or any parsing, just a basic extract and load.
  */
object ImportRawToParquet extends TypedSparkScriptIncremental[DocumentConfig[CustomerInputFiles]] {
  val name = "ImportRawToParquet - Customer"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: DocumentConfig[CustomerInputFiles], etlMetricsRepository: ETLMetricsRepository, metadataRunId: MetadataRunId): Unit = {

    import spark.implicits._

    val destinationPath = projectConfig.parquetRoot
    val customerPath = projectConfig.inputFiles.customer
    val accountPath = projectConfig.inputFiles.account
    val accToCusPath = projectConfig.inputFiles.acc_to_cus

    val customerReader = new ReaderWithSchemaValidation[CustomerRaw] {
      def df(path: String): DataFrame = readCSV(path, spark)
    }
    val accountReader = new ReaderWithSchemaValidation[AccountRaw] {
      def df(path: String): DataFrame = readCSV(path, spark)
    }
    val accToCusReader = new ReaderWithSchemaValidation[AccToCusRaw] {
      def df(path: String): DataFrame = readCSV(path, spark)
    }

    // read customer CSV file, validate schema, convert to dataset and then write out typed parquet file
    convertAndValidateCSV(customerReader, customerPath, destinationPath, spark, logger)
    // read account CSV file, validate schema, convert to dataset and then write out typed parquet file
    convertAndValidateCSV(accountReader, accountPath, destinationPath, spark, logger)
    // read acc_to_cus CSV file, validate schema, convert to dataset and then write out typed parquet file
    convertAndValidateCSV(accToCusReader, accToCusPath, destinationPath, spark, logger)
  }
}
