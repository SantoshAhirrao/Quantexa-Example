package com.quantexa.example.generators

import com.quantexa.example.generators.config.GeneratorConfig
import com.quantexa.generators.utils.GeneratorUtils._
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import io.circe.generic.auto._

/**
  * Given a generatorConfig this script will create random test data representations of customer.csv, acc_to_cus.csv, account.csv and hotlist.csv
  * Documentation is available at https://quantexa.atlassian.net/wiki/spaces/PE/pages/1329791027/Generating+Test+Data
  */
object AllRawDataGenerator extends TypedSparkScript[GeneratorConfig] {

  val name = "AllRawDataGenerator"

  val scriptDependencies = Set.empty[QuantexaSparkScript]
  val fileDependencies = Map.empty[String, String]

  protected def run(spark: SparkSession, logger: Logger, args: Seq[String], config: GeneratorConfig,
                    etlMetricsRepository: ETLMetricsRepository) = {

    val specificEntities = generateCommonEntities(
      config.numberOfPersonEntities.getOrElse(750),
      config.numberOfAddressEntities.getOrElse(750),
      config.numberOfBusinessEntities.getOrElse(750),
      config.numberOfPhoneEntities.getOrElse(750),
      config.numberOfEmailEntities.getOrElse(750),
      config.seed.getOrElse(new scala.util.Random().nextInt)
    )

    logger.info("Generated Entities")

    RawCustomerDataGenerator.generateAndWriteCustomerRecords(spark, config, specificEntities)
    RawHotlistDataGenerator.generateAndWriteHotlistRecords(spark, config, specificEntities)
    RawTransactionDataGenerator.generateAndWriteTransactionRecords(spark, logger, config, specificEntities)
  }
}