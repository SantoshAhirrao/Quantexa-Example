package com.quantexa.example.generators

import com.quantexa.example.generators.arbitrary.customer.{AccToCusRawGenerator, AccountRawGenerator, CustomerRawGenerator}
import com.quantexa.example.generators.config.GeneratorConfig
import com.quantexa.generators.utils.GeneratedEntities
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.auto._
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import com.quantexa.generators.utils.GeneratorUtils.generateCommonEntities
import com.quantexa.etl.utils.spark.SparkUtilityFunctions.writeToSingleCSV

object RawCustomerDataGenerator extends TypedSparkScript[GeneratorConfig] {

  val name = "RawCustomerDataGenerator"

  val scriptDependencies = Set.empty[QuantexaSparkScript]
  val fileDependencies = Map.empty[String, String]

  protected def run(spark: SparkSession, logger: Logger, args: Seq[String], config: GeneratorConfig,
                    etlMetricsRepository: ETLMetricsRepository) = {

    val specificEntities = generateCommonEntities(200,200,200,200,200, 21)

    generateAndWriteCustomerRecords(spark, config, specificEntities)
  }

  def generateAndWriteCustomerRecords(spark:SparkSession, config: GeneratorConfig, generatedEntities: GeneratedEntities): Unit = {
    import spark.implicits._
    import com.quantexa.example.generators.config.GeneratorLoaderImplicits._

    val customerRawGenerator = new CustomerRawGenerator(config, generatedEntities)
    val accountRawGenerator = new AccountRawGenerator(config, generatedEntities)
    val accToCusRawGenerator = new AccToCusRawGenerator(config)

    val customerDataset = customerRawGenerator.generateRecords.toDS
    val accountDataset = accountRawGenerator.generateRecords.toDS
    val accToCusDataset = accToCusRawGenerator.generateRecords.toDS

    writeToSingleCSV(customerDataset, config.rootHDFSPath + "customer/raw/csv/customer.csv")
    writeToSingleCSV(accountDataset, config.rootHDFSPath + "customer/raw/csv/account.csv")
    writeToSingleCSV(accToCusDataset, config.rootHDFSPath + "customer/raw/csv/acc_to_cus.csv")
  }
}
