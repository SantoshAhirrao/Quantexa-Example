package com.quantexa.example.etl.projects.fiu.transaction

import java.sql.Date

import com.quantexa.etl.utils.cleansing.Business
import com.quantexa.etl.utils.cleansing.Business.isBusiness
import com.quantexa.etl.utils.cleansing.Individual.parseName
import com.quantexa.example.etl.projects.fiu.{DocumentConfig, TransactionInputFile}
import com.quantexa.example.model.fiu.transaction.TransactionModel._
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScriptIncremental}
import io.circe.generic.auto._
import org.apache.log4j.Logger
import org.apache.spark.sql._
import com.quantexa.example.model.fiu.customer.CustomerRawModel.AccToCusRaw

/***
  * QuantexaSparkScript used to parse the hierarchical model components for the transaction data
  * Input: DocumentDataModel/CleansedDocumentDataModel.parquet
  * Output: /DocumentDataModel/AggregatedCleansedDocumentDataModel.parquet
  *
  * Stage 4
  * At this stage we aggregate all transactions between each pair of accounts
  *
  */
object CreateAggregationClass extends TypedSparkScriptIncremental[DocumentConfig[TransactionInputFile]] {
  val name = "CreateCaseClass - AggregatedTransaction"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: DocumentConfig[TransactionInputFile], etlMetricsRepository: ETLMetricsRepository, metadataRunId: MetadataRunId): Unit = {
    if (args.nonEmpty) {
      logger.warn(args.length + " arguments were passed to the script and are being ignored")
    }

    import spark.implicits._

    val inputPath = projectConfig.cleansedCaseClassPath
    val destinationPath = projectConfig.aggregatedCaseClassPath
      .getOrElse(throw new IllegalStateException("AggregatedCaseClass path is not set in config."))
    val cleansedTransactionDS = spark.read.parquet(inputPath).as[Transaction]

    val accToCusRawDS = spark.read.parquet(s"${projectConfig.parquetRoot}/acc_to_cus.parquet").as[AccToCusRaw]
      .filter(accToCus => accToCus.prim_acc_no.isDefined && accToCus.cus_id_no.isDefined)

    /* If using 1 joinWith and an OR condition, Spark will silently hang and on a BroadcastNestedLoopJoin.
     * Rather than turning off broadcast joins, 2 joinWiths stops Spark from hanging and utilises a fast BroadcastHashJoin. */
    val transactionJoinedWithCustomerId: Dataset[(Transaction, AccToCusRaw)] = cleansedTransactionDS
      .joinWith(accToCusRawDS,
        cleansedTransactionDS("debitCredit") === "D" && cleansedTransactionDS("originatingAccount.accountNumber") === accToCusRawDS("prim_acc_no")
        , "inner").union(cleansedTransactionDS
      .joinWith(accToCusRawDS,
          cleansedTransactionDS("debitCredit") === "C" && cleansedTransactionDS("beneficiaryAccount.accountNumber") === accToCusRawDS("prim_acc_no")
        , "inner"))

    etlMetricsRepository.size("transactionsDS", transactionJoinedWithCustomerId.count())

    val aggregatedTransactionDS = transactionJoinedWithCustomerId.groupByKey {
      //Filtering from above ensures these `.get` calls are Safe
      case (txn: Transaction, accountToCustomer: AccToCusRaw) =>
        val counterpartyAccountId = txn.debitCredit match {
          case "D" => txn.beneficiaryAccount.get.accountId
          case "C" => txn.originatingAccount.get.accountId
          case _ => txn.beneficiaryAccount.get.accountId
        }
        (accountToCustomer.cus_id_no.get, counterpartyAccountId)
    }.mapGroups {
      case ((customerId, counterpartyAccountId), txnsSeq) =>

        val txns: Seq[Transaction] = txnsSeq.map(_._1).toSeq
        val txn: Transaction = txns.head
        val beneAcct = txn.beneficiaryAccount.get
        val origAcct = txn.originatingAccount.get
        val beneAccId = beneAcct.accountId
        val origAccId = origAcct.accountId
        val beneAccName = beneAcct.accountName
        val origAccName = origAcct.accountName

        val originAccountCountry = (for {
          txn <- txns
          acc <- txn.originatingAccount
          accountCountry <- acc.accountCountry
        } yield accountCountry).headOption

        val beneficiaryAccountCountry = (for {
          txn <- txns
          acc <- txn.beneficiaryAccount
          accountCountry <- acc.accountCountry
        } yield accountCountry).headOption

        val isCrossBorder = for {
          originAccountCountryValue <- originAccountCountry
          beneficiaryAccountCountryValue <- beneficiaryAccountCountry
        } yield originAccountCountryValue != beneficiaryAccountCountryValue

        val currencies = (txns.map {_.txnCurrency} ++ txns.map {_.baseCurrency}).distinct

        val beneficiaryParty: AggregatedTransactionParty = parseAggregatedParty(AggregatedTransactionParty(
          beneAccId, beneAccName, None, None
        ))

        val beneficiaryAggregatedTransactionAccount: AggregatedTransactionAccount =
          AggregatedTransactionAccount(beneAccId, beneAccId, beneficiaryParty)

        val originatingParty: AggregatedTransactionParty = parseAggregatedParty(AggregatedTransactionParty(
          origAccId, origAccName, None, None
        ))

        val originatingAggregatedTransactionAccount: AggregatedTransactionAccount =
          AggregatedTransactionAccount(origAccId, origAccId, originatingParty)

        implicit def dateOrderer: Ordering[Date] = new Ordering[Date] {
          def compare(x: Date, y: Date): Int = x compareTo y
        }

        val sortedDates = txns.map(t => new Date(t.postingDate.getTime)).sorted
        val firstTransactionDate = sortedDates.head
        val lastTransactionDate = sortedDates.last

        val aggTxnStats: AggregatedStats = computeStatistics(txns)

        AggregatedTransaction(
          customerId.toString.concat("|").concat(counterpartyAccountId),
          customerId,
          originatingAggregatedTransactionAccount,
          beneficiaryAggregatedTransactionAccount,
          origAccName,
          beneAccName,
          isCrossBorder,
          Some(txn.debitCredit),
          currencies,
          Some(aggTxnStats),
          Some(firstTransactionDate),
          Some(lastTransactionDate)
        )
    }

    etlMetricsRepository.time("CreateCaseClass", aggregatedTransactionDS.write.mode("overwrite")).parquet(destinationPath)
    etlMetricsRepository.size("transaction case class", spark.read.parquet(destinationPath).count())
  }

  def parseAggregatedParty(party: AggregatedTransactionParty): AggregatedTransactionParty = {
    val (individualNames, businessName) = if (isBusiness(party.partyName)) {
      (None, Business.parse(party.partyName).map(_.toParentVersion[AggregatedTransactionParty]))
    }
    else {
      (parseName(party.partyName).map(_.map(_.toParentVersion[AggregatedTransactionParty]).toSeq), None)
    }

    party.copy(
      parsedIndividualNames = individualNames,
      parsedBusinessNames = businessName
    )
  }

  def computeStatistics(txns: Seq[Transaction]): AggregatedStats = {
    if (txns.isEmpty) {
      AggregatedStats(None, None, None, None, None, None, None)
    } else {
      val txnAmounts = txns.map(_.txnAmount)
      val txnAmountsBaseCurrency = txns.map(_.txnAmountInBaseCurrency)
      val txnSum = txnAmounts.sum
      val txnCount = txns.size
      val txnMin = txnAmounts.min
      val txnMax = txnAmounts.max
      val txnSumBase = txnAmountsBaseCurrency.sum
      val txnMinBase = txnAmountsBaseCurrency.min
      val txnMaxBase = txnAmountsBaseCurrency.max

      AggregatedStats(Some(txnSum), Some(txnCount), Some(txnMin), Some(txnMax),
        Some(txnSumBase), Some(txnMinBase), Some(txnMaxBase))
    }
  }
}
