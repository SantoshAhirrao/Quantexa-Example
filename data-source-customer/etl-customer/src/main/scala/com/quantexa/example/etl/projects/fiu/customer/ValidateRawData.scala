package com.quantexa.example.etl.projects.fiu.customer

import com.quantexa.example.etl.projects.fiu.{CustomerInputFiles, DocumentConfig}
import com.quantexa.example.model.fiu.customer.CustomerRawModel.{AccToCusRaw, AccountRaw, CustomerRaw}
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScriptIncremental}
import com.quantexa.etl.validation.row.ValidationUtils.validateRaw
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.log4j.Logger
import io.circe.generic.auto.exportDecoder

/**
  * Script to validate raw input data. Validation rules are defined within the companion object for the data model.
  * Validation rules are applied to each row of the input data.
  *
  * Outputs:
  *   validation_failures.parquet  (contains failed raw data, together with the validation rules causing the failure)
  *
  * Additionally, if there are input rows failing validation checks:
  *   summary_stats.csv -- simple stats showing which validation rules failed, and on which input columns.
  *   customer_validated.parquet, account_validated.parquet, acc_to_cus_validated.parquet -- the validated raw data.
  */
object ValidateRawData extends TypedSparkScriptIncremental[DocumentConfig[CustomerInputFiles]] {
  val name = "ValidateRawData - Customer"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          logger: Logger,
          args: Seq[String],
          projectConfig: DocumentConfig[CustomerInputFiles],
          etlMetricsRepository: ETLMetricsRepository,
          metadataRunId: MetadataRunId): Unit = {

    import spark.implicits._

    val parquetRoot = projectConfig.parquetRoot
    val customerPath = parquetRoot + "/customer.parquet"
    val accountPath = parquetRoot + "/account.parquet"
    val accToCusPath = parquetRoot + "/acc_to_cus.parquet"

    val customerRaw = spark.read.parquet(customerPath)
      .as[CustomerRaw]
    val accountRaw = spark.read.parquet(accountPath)
      .as[AccountRaw]
    val accToCusRaw = spark.read.parquet(accToCusPath)
      .as[AccToCusRaw]


    val validatedCustomer = validateRaw(customerRaw, logger)
    val validatedAccount = validateRaw(accountRaw, logger)
    val validatedAccToCus = validateRaw(accToCusRaw, logger)

    val validatedParquetPath = projectConfig.validatedParquetPath

    validatedCustomer.write.mode(SaveMode.Overwrite).parquet(s"$validatedParquetPath/customer.parquet")
    validatedAccount.write.mode(SaveMode.Overwrite).parquet(s"$validatedParquetPath/account.parquet")
    validatedAccToCus.write.mode(SaveMode.Overwrite).parquet(s"$validatedParquetPath/acc_to_cus.parquet")
  }
}
