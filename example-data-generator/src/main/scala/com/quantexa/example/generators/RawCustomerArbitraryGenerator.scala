package com.quantexa.example.generators

import com.quantexa.example.model.fiu.customer.CustomerRawModel.{AccToCusRaw, AccountRaw, CustomerRaw}
import com.quantexa.generators.utils.ArbitraryUtils
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{NoConfigSparkScript, QuantexaSparkScript}
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

/**
  * This generates a template arbitrary for the RawCustomer class
  * More information about the arbitrary generator scripts is available at https://quantexa.atlassian.net/wiki/spaces/TECH/pages/59736065/Generating+Test+Data+for+Projects
  * With information about how to populate your arbitrary template available at https://quantexa.atlassian.net/wiki/spaces/TECH/pages/60751935/Creating+Arbitrary+Guide
  */
object RawCustomerArbitraryGenerator extends NoConfigSparkScript {

  val name = "RawCustomerArbitraryGenerator"

  val scriptDependencies = Set.empty[QuantexaSparkScript]
  val fileDependencies = Map.empty[String, String]

  protected def run(spark: SparkSession, logger: Logger, args: Seq[String],
                    etlMetricsRepository: ETLMetricsRepository) = {

    ArbitraryUtils.printCaseClassSchema[CustomerRaw]
    ArbitraryUtils.printCaseClassSchema[AccountRaw]
    ArbitraryUtils.printCaseClassSchema[AccToCusRaw]
  }
}
