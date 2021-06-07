package com.quantexa.example.etl.projects.fiu.customer

import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils.outputDatasetToCSV
import com.quantexa.example.etl.projects.fiu.utils.SharedTestUtils.ETLConfig
import com.quantexa.example.etl.projects.fiu.customer.CustomerTestData.{accountData, accountToCustomerData, accountToCustomerUpdateData, accountUpdateData, customerData, customerUpdateData}
import com.quantexa.example.model.fiu.customer.CustomerRawModel.{AccToCusRaw, AccountRaw, CustomerRaw}
import com.quantexa.scriptrunner.util.CommandLine
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import com.typesafe.config.ConfigFactory


object CustomerTestUtils {
  def extractTransform(spark: SparkSession, logger: Logger, metrics: ETLMetricsRepository, dataRootDir: String): Unit = {

    val cmdLine = CommandLine().copy(configRoot = Some("etl.customer"))

    sys.props += (("environment.fileSystemRoot", dataRootDir))
    ImportRawToParquet.executeScript(spark, logger, cmdLine, metrics)
    ValidateRawData.executeScript(spark, logger, cmdLine, metrics)
    CreateCaseClass.executeScript(spark, logger, cmdLine, metrics)
    CleanseCaseClass.executeScript(spark, logger, cmdLine, metrics)

    val cmdLineCompounds = CommandLine().copy(configRoot = Some("compounds.customer"))
    CreateCompounds.executeScript(spark, logger, cmdLineCompounds, metrics)
  }

  def extractTransformIncremental(spark: SparkSession, logger: Logger, metrics: ETLMetricsRepository, dataRootDir: String): Unit = {

    val cmdLine = CommandLine().copy(configRoot = Some("etl.customer"))

    sys.props += (("environment.inputDataFileSystemRoot", s"$dataRootDir/update"), ("environment.fileSystemRoot", dataRootDir))
    ConfigFactory.invalidateCaches()

    val etlConfig = ETLConfig(dataRootDir, None, "run_2")

    GenerateMetadataRow.run(spark, logger, Seq(), etlConfig, metrics)
    ImportRawToParquet.executeScript(spark, logger, cmdLine, metrics)
    ValidateRawData.executeScript(spark, logger, cmdLine, metrics)
    CreateCaseClass.executeScript(spark, logger, cmdLine, metrics)
    CreateCaseClassDelta.executeScript(spark, logger, cmdLine, metrics)
    CleanseCaseClass.executeScript(spark, logger, cmdLine, metrics)

    val cmdLineCompounds = CommandLine().copy(configRoot = Some("compounds.customer"))
    CreateCompounds.executeScript(spark, logger, cmdLineCompounds, metrics)
  }

  def writeCustomerFilesCSV(dataRootDir: String): Unit ={

    val customerFilesDir = dataRootDir + "/customer/raw/csv"
    val customerUpdateFilesDir = dataRootDir + "/update/customer/raw/csv"

    outputDatasetToCSV[CustomerRaw](customerData, customerFilesDir + "/customer.csv")
    outputDatasetToCSV[AccountRaw](accountData, customerFilesDir + "/account.csv")
    outputDatasetToCSV[AccToCusRaw](accountToCustomerData, customerFilesDir + "/acc_to_cus.csv")
    outputDatasetToCSV[CustomerRaw](customerUpdateData, customerUpdateFilesDir + "/customer.csv")
    outputDatasetToCSV[AccountRaw](accountUpdateData, customerUpdateFilesDir + "/account.csv")
    outputDatasetToCSV[AccToCusRaw](accountToCustomerUpdateData, customerUpdateFilesDir + "/acc_to_cus.csv")
  }
}