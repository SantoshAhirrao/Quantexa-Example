package com.quantexa.example.etl.projects.fiu.transaction

import com.quantexa.example.etl.projects.fiu.{DocumentConfig, ETLConfig, TransactionInputFile}
import com.quantexa.example.etl.projects.fiu.transaction.helpers.CountryHelper
import com.quantexa.example.model.fiu.transaction.TransactionModel
import com.quantexa.example.model.fiu.transaction.TransactionModel.{Transaction, TransactionAccount}
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScriptIncremental}
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import com.quantexa.scriptrunner.util.incremental.MetaDataRepositoryImpl._
import org.apache.spark.sql.{AnalysisException, SparkSession}

import scala.util.{Failure, Success, Try}

/***
  * QuantexaSparkScript used to parse the hierarchical model components for the transaction data
  * Input: DocumentDataModel/DocumentDataModel.parquet (or DocumentDataModel/DocumentDataModel_Updated.parquet after initial run)
  * Output: DocumentDataModel/CleansedDocumentDataModel.parquet
  *
  * Stage 3
  * At this stage we want to parse the raw values of the hierarchical model and populate the model as best we can
  *
  */
object CleanseCaseClass extends TypedSparkScriptIncremental[DocumentConfig[TransactionInputFile]] {
  val name: String = "CleanseCaseClass - Transaction"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: DocumentConfig[TransactionInputFile], etlMetricsRepository: ETLMetricsRepository, metadataRunId: MetadataRunId): Unit = {
    if (args.nonEmpty) {
      logger.warn(args.length + " arguments were passed to the script and are being ignored")
    }
    import spark.implicits._

    val caseClassPath = projectConfig.caseClassPath
    val caseClassUpdatedPath = caseClassPath.replace("DocumentDataModel.parquet", "DocumentDataModel_Updated.parquet")
    val destinationPath = projectConfig.cleansedCaseClassPath

    val caseClassDS = Try(spark.read.parquet(caseClassUpdatedPath).as[Transaction]) match {
      case Success(txnDS) => txnDS
      case Failure(e : AnalysisException) => spark.read.parquet(caseClassPath).as[Transaction]
      case Failure(exception) =>
        logger.error(s"Failed to load customer case class data. Paths checked: \n $caseClassPath \n $caseClassUpdatedPath")
        throw exception
    }

    etlMetricsRepository.size("case class", caseClassDS.count)

    val parsedTransactionDS = caseClassDS
      .map(parseTransaction)
      .map(_.copy(metaData = Some(metadataRunId)))

    etlMetricsRepository.size("Cleansed case class", parsedTransactionDS.count)
    etlMetricsRepository.time("CleanseCaseClass", parsedTransactionDS.write.mode("overwrite").parquet(destinationPath))
  }

  def parseTransaction(transaction: TransactionModel.Transaction) = {
    transaction.copy(
      beneficiaryAccount = transaction.beneficiaryAccount.map(account => {
        account.copy(accountCountryISO3Code = cleanseCountry(account))
      }),
      originatingAccount = transaction.originatingAccount.map(account => {
        account.copy(accountCountryISO3Code = cleanseCountry(account))
      }),
      txnCurrency = mapToCurrencyCode(transaction.txnCurrency),
      baseCurrency = mapToCurrencyCode(transaction.baseCurrency)
    )
  }

  def cleanseCountry(account: TransactionAccount): Option[String] = {
   account.accountCountry.map(country => CountryHelper.convertIso2ToIso3(country))
  }

  def mapToCurrencyCode = Map(
    "Stirling"  -> "GBP",
    "Euro"      -> "EUR",
    "Dollar"    -> "USD"
  )
}
