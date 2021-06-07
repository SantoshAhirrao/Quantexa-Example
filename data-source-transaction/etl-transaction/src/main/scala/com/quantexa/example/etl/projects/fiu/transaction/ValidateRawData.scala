package com.quantexa.example.etl.projects.fiu.transaction

import com.quantexa.etl.validation.row.ValidationUtils.validateRaw
import com.quantexa.example.etl.projects.fiu.{DocumentConfig, TransactionInputFile}
import com.quantexa.example.model.fiu.customer.CustomerRawModel.AccToCusRaw
import com.quantexa.example.model.fiu.transaction.TransactionRawModel.TransactionRaw
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScriptIncremental}
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.{SaveMode, SparkSession}

/**
  * Script to validate raw input data. Validation rules are defined within the companion object for the data model.
  * Validation rules are applied to each row of the input data.
  *
  * Outputs:
  *   validation_failures.parquet  (contains failed raw data, together with the validation rules causing the failure)
  *
  * Additionally, if there are input rows failing validation checks:
  *   summary_stats.csv -- simple stats showing which validation rules failed, and on which input columns.
  *   transaction_validated.parquet, account_validated.parquet, acc_to_cus_validated.parquet -- the validated raw data.
  */
object ValidateRawData extends TypedSparkScriptIncremental[DocumentConfig[TransactionInputFile]] {
  val name = "ValidateRawData - Transaction"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          logger: Logger,
          args: Seq[String],
          projectConfig: DocumentConfig[TransactionInputFile],
          etlMetricsRepository: ETLMetricsRepository,
          metadataRunId: MetadataRunId): Unit = {

    import spark.implicits._

    val parquetRoot = projectConfig.parquetRoot
    val transactionPath = parquetRoot + "/transactions.parquet"
    val accToCusPath = parquetRoot + "/acc_to_cus.parquet"

    val transactionRaw = spark.read.parquet(transactionPath)
      .as[TransactionRaw]
    val accToCusRaw = spark.read.parquet(accToCusPath)
      .as[AccToCusRaw]


    val validatedTransaction = validateRaw(transactionRaw, logger)
    val validatedAccToCus = validateRaw(accToCusRaw, logger)

    val validatedParquetPath = projectConfig.validatedParquetPath

    validatedTransaction.write.mode(SaveMode.Overwrite).parquet(s"$validatedParquetPath/transactions.parquet")
    validatedAccToCus.write.mode(SaveMode.Overwrite).parquet(s"$validatedParquetPath/acc_to_cus.parquet")
  }
}
