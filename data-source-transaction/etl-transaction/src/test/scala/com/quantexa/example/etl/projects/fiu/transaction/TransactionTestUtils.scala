package com.quantexa.example.etl.projects.fiu.transaction

import com.quantexa.example.etl.projects.fiu.transaction.TransactionTestData.{accountToCustomerData, accountToCustomerUpdateData, transactionData, transactionUpdateData}
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils.outputDatasetToCSV
import com.quantexa.example.etl.projects.fiu.utils.SharedTestUtils.ETLConfig
import com.quantexa.example.model.fiu.customer.CustomerRawModel.AccToCusRaw
import com.quantexa.example.model.fiu.transaction.TransactionRawModel.TransactionRaw
import com.quantexa.scriptrunner.util.CommandLine
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession


object TransactionTestUtils {
  def extractTransform(spark: SparkSession, logger: Logger, metrics: ETLMetricsRepository, dataRootDir: String): Unit = {

    val cmdLine = CommandLine().copy(configRoot = Some("etl.transaction"))
    sys.props += (("environment.fileSystemRoot", dataRootDir))

    ImportRawToParquet.executeScript(spark, logger, cmdLine, metrics)
    ValidateRawData.executeScript(spark, logger, cmdLine, metrics)
    CreateCaseClass.executeScript(spark, logger, cmdLine, metrics)
    CleanseCaseClass.executeScript(spark, logger, cmdLine, metrics)
    CreateAggregationClass.executeScript(spark, logger, cmdLine, metrics)

    val cmdLineCompounds = CommandLine().copy(configRoot = Some("compounds.transaction"))
    CreateCompounds.executeScript(spark, logger, cmdLineCompounds, metrics)
  }

  def extractTransformIncremental(spark: SparkSession, logger: Logger, metrics: ETLMetricsRepository, dataRootDir: String): Unit = {

    val cmdLine = CommandLine().copy(configRoot = Some("etl.transaction"))

    sys.props += (("environment.inputDataFileSystemRoot", s"$dataRootDir/update"), ("environment.fileSystemRoot", dataRootDir))
    ConfigFactory.invalidateCaches()

    val etlConfig = ETLConfig(dataRootDir, None, "run_2")

    GenerateMetadataRow.run(spark, logger, Seq(), etlConfig, metrics)
    ImportRawToParquet.executeScript(spark, logger, cmdLine, metrics)
    ValidateRawData.executeScript(spark, logger, cmdLine, metrics)
    CreateCaseClass.executeScript(spark, logger, cmdLine, metrics)
    CreateCaseClassDelta.executeScript(spark, logger, cmdLine, metrics)
    CleanseCaseClass.executeScript(spark, logger, cmdLine, metrics)
    CreateAggregationClass.executeScript(spark, logger, cmdLine, metrics)
    CreateAggregationClassDelta.executeScript(spark, logger, cmdLine, metrics)

    val cmdLineCompounds = CommandLine().copy(configRoot = Some("compounds.transaction"))
    CreateCompounds.executeScript(spark, logger, cmdLineCompounds, metrics)
  }

  def writeTransactionCSV(dataRootDir: String): Unit ={

    val transactionCSVDir = dataRootDir + "/transaction/raw/csv/transactions.csv"
    val transactionUpdateCSVDir = dataRootDir + "/update/transaction/raw/csv/transactions.csv"
    val customerCSVDir = dataRootDir + "/customer/raw/csv/acc_to_cus.csv"
    val customerUpdateCSVDir = dataRootDir + "/update/customer/raw/csv/acc_to_cus.csv"

    outputDatasetToCSV[TransactionRaw](transactionData, transactionCSVDir)
    outputDatasetToCSV[TransactionRaw](transactionUpdateData, transactionUpdateCSVDir)
    outputDatasetToCSV[AccToCusRaw](accountToCustomerData,customerCSVDir)
    outputDatasetToCSV[AccToCusRaw](accountToCustomerUpdateData, customerUpdateCSVDir)
  }

}
