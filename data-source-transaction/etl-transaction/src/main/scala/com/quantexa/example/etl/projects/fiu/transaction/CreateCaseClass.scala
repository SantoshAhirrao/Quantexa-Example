package com.quantexa.example.etl.projects.fiu.transaction

import com.quantexa.example.etl.projects.fiu.{DocumentConfig, ETLConfig, TransactionInputFile}
import com.quantexa.example.model.fiu.transaction.TransactionModel._
import com.quantexa.example.model.fiu.transaction.TransactionRawModel.TransactionRaw
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript, TypedSparkScriptIncremental}
import io.circe.generic.auto._
import org.apache.log4j.Logger
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils.timestampToDate
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.util.incremental.MetaDataRepositoryImpl._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

/**
 * *
 * QuantexaSparkScript used to cleanse validated Parquet files and create the hierarchical model for the FIU Smoke Test Data
 * Output: DocumentDataModel/DocumentDataModel.parquet
 *
 * Stage 2
 * At this stage we want to cleanse the nullable types from our dataset such as unwanted empty strings. We also
 * want to enforce types by converting the raw types for example a string of date format "yyyy/MM/dd" to a
 * java.sql.Date.
 * We may also want to remove any miscellaneous artifacts such as rows with null non-nullable values.
 * Once cleansed we want to construct the hierarchical complex model through the use of joins.
 *
 */
object CreateCaseClass extends TypedSparkScriptIncremental[DocumentConfig[TransactionInputFile]] {
  val name = "CreateCaseClass - Transaction"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: DocumentConfig[TransactionInputFile], etlMetricsRepository: ETLMetricsRepository, metadataRunId: MetadataRunId): Unit = {
    if (args.nonEmpty) {
      logger.warn(args.length + " arguments were passed to the script and are being ignored")
    }

    import spark.implicits._

    val transactionPath = projectConfig.validatedParquetPath + "/transactions.parquet"
    val destinationPath = projectConfig.caseClassPath
    val transactionRawDS = spark.read.parquet(transactionPath).as[TransactionRaw]

    // metrics
    etlMetricsRepository.size("transactionsRawDS", transactionRawDS.count())

    val transactionDS = transactionRawDS.map(rawTransaction => {
        val beneAccountId = rawTransaction.bene_acc_no.get
        val beneAccountName = rawTransaction.bene_name
        val beneficiaryBankCountry = rawTransaction.bene_bank_ctry
        val beneficiaryTransactionAccount = Some(TransactionAccount(
          beneAccountId.toString, beneAccountId.toString, beneAccountName, None, None, None, None, beneficiaryBankCountry, None))

        val origAccountId = rawTransaction.orig_acc_no.get
        val origAccountName = rawTransaction.orig_name
        val originatingBankCountry = rawTransaction.orig_ctry
        val originatingTransactionAccount = Some(TransactionAccount(
          origAccountId.toString, origAccountId.toString, origAccountName, None, None, None, None, originatingBankCountry, None))

        Transaction(
          transactionId = rawTransaction.txn_id.get,
          originatingAccount = originatingTransactionAccount,
          beneficiaryAccount = beneficiaryTransactionAccount,
          debitCredit = rawTransaction.cr_dr_cde.get,
          postingDate = rawTransaction.posting_dt.get,
          runDate = rawTransaction.run_dt.map(timestampToDate).get,
          txnAmount = rawTransaction.txn_amt.get,
          txnCurrency = rawTransaction.txn_currency.get,
          txnAmountInBaseCurrency = rawTransaction.txn_amt_base.get,
          baseCurrency = rawTransaction.base_currency.get,
          txnDescription = rawTransaction.txn_desc,
          sourceSystem = rawTransaction.source_sys,
          metaData = Some(metadataRunId)
        )
      }
    )

    etlMetricsRepository.size("transaction case class", transactionDS.count())
    etlMetricsRepository.time("CreateCaseClass", transactionDS.write.mode("overwrite")).parquet(destinationPath)
  }
}