package com.quantexa.example.generators

import com.quantexa.example.generators.arbitrary.transaction.TransactionRawGenerator
import com.quantexa.example.generators.arbitrary.transaction.TransactionDataVerifier.checkGeneratedTransactions
import com.quantexa.example.generators.config.GeneratorConfig
import com.quantexa.generators.utils.GeneratedEntities
import com.quantexa.generators.utils.GeneratorUtils.generateCommonEntities
import com.quantexa.etl.utils.spark.SparkUtilityFunctions.writeToSingleCSV
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.auto._
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

object RawTransactionDataGenerator extends TypedSparkScript[GeneratorConfig] {

  val name = "RawTransactionDataGenerator"

  val scriptDependencies = Set.empty[QuantexaSparkScript]
  val fileDependencies = Map.empty[String, String]

  protected def run(spark: SparkSession, logger: Logger, args: Seq[String], config: GeneratorConfig,
                    etlMetricsRepository: ETLMetricsRepository) = {

    val specificEntities = generateCommonEntities(200,200,200,200,200, 21)

    generateAndWriteTransactionRecords(spark, logger, config, specificEntities)
  }

  def generateAndWriteTransactionRecords(spark: SparkSession, logger: Logger, config: GeneratorConfig, generatedEntities: GeneratedEntities): Unit = {
    import com.quantexa.example.generators.config.GeneratorLoaderImplicits._
    import spark.implicits._

    val transactionRawGenerator = new TransactionRawGenerator(spark, logger, config, generatedEntities)

    logger.info("Generated Transactions Class")

    val transactionDataset = transactionRawGenerator.generateRecords.toDS

    logger.info("Generated Dataset of Transactions")

    //Does validation on the distribution of the types of transaction
    checkGeneratedTransactions(spark, logger, config, transactionRawGenerator.muleAccount, transactionRawGenerator.internalAccountIds, transactionDataset)

    logger.info("Validated Transactions")

    writeToSingleCSV(transactionDataset, config.rootHDFSPath + "transaction/raw/csv/transactions.csv")

    logger.info("Transactions Generated")
  }
}
