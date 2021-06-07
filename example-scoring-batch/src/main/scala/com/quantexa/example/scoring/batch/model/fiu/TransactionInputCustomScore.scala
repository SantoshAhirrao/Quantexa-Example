package com.quantexa.example.scoring.batch.model.fiu

import java.time.temporal.ChronoUnit

import com.quantexa.example.model.fiu.transaction.TransactionModel.{Transaction, TransactionAccount}
import com.quantexa.example.model.fiu.transaction.scoring.{ScoreableTransaction, TransactionWithCustomerIds}
import com.quantexa.example.scoring.model.fiu.ScoringModel.CountryCorruption
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.spark.model.scores.CustomScore
import com.quantexa.scriptrunner.util.DevelopmentConventions.FolderConventions
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Dataset, SparkSession}


trait TransactionInputCustomScore extends CustomScore {

  def config: ProjectExampleConfig

  /** Fact creation logic goes in here */
  def processScoreableTransaction(spark: SparkSession, transactions: Dataset[ScoreableTransaction])(implicit scoreInput: ScoreInput): Any

  final def score(spark: SparkSession)(implicit scoreInput: ScoreInput): Any = {
    import spark.implicits._
    val transactionPath = FolderConventions.cleansedCaseClassFile(config.hdfsFolderTransactions)

    val transactions = spark.read.parquet(transactionPath).as[Transaction]

    val scoreableTransactions = createScoreableTransaction(transactions)

    processScoreableTransaction(spark, scoreableTransactions)
  }

  private final def createScoreableTransaction(ds: Dataset[Transaction]): Dataset[ScoreableTransaction] = {

    val spark = ds.sparkSession
    import spark.implicits._

    val customerCleansed = spark.read.parquet(FolderConventions.cleansedCaseClassFile(config.hdfsFolderCustomer))
    val customerAccountIds = customerCleansed.select($"customerIdNumberString", explode($"accounts").as("account"))
      .select($"customerIdNumberString".as("customerId"), $"account.primaryAccountNumber".as("accountId"))

    /* this is necessary to avoid the spark trivally true bug that misintereprets self joined tables as cartesian products
   https://stackoverflow.com/questions/32190828/spark-sql-performing-carthesian-join-instead-of-inner-join */

    val customerCleansed2 = spark.read.parquet(FolderConventions.cleansedCaseClassFile(config.hdfsFolderCustomer))
    val customerAccountIds2 = customerCleansed2.select($"customerIdNumberString", explode($"accounts").as("account"))
      .select($"customerIdNumberString".as("customerId_2"), $"account.primaryAccountNumber".as("accountId_2"))

    val transactionWithCustomerIds = ds
      .join(customerAccountIds,
        ds("debitCredit") === "D" && ds("originatingAccount.accountNumber") === customerAccountIds("accountId")
        , "inner").union(ds
      .join(customerAccountIds,
        ds("debitCredit") === "C" && ds("beneficiaryAccount.accountNumber") === customerAccountIds("accountId")
        , "inner"))
      .withColumn("counterpartyAccountId",
        when($"debitCredit" === "D", $"beneficiaryAccount.accountId").otherwise(
          when($"debitCredit" === "C", $"originatingAccount.accountId").otherwise(
            $"beneficiaryAccount.accountId"
          )
        )
      )
      .drop("accountId")
      .join(customerAccountIds2,
        $"counterpartyAccountId" === customerAccountIds2("accountId_2"),
        "left")
      .withColumnRenamed("customerId_2", "counterpartyCustomerId")
      .drop("accountId_2")
      .as[TransactionWithCustomerIds]


    //TODO: Externalise all instances of this
    val countryCorruption = spark.read.parquet(s"${config.hdfsFolderCountryCorruption}/raw/parquet/CorruptionIndex.parquet").as[CountryCorruption]
    /* this is necessary to avoid the spark trivally true bug that misintereprets self joined tables as cartesian products
       https://stackoverflow.com/questions/32190828/spark-sql-performing-carthesian-join-instead-of-inner-join */
    val countryCorruption1 = spark.read.parquet(s"${config.hdfsFolderCountryCorruption}/raw/parquet/CorruptionIndex.parquet").as[CountryCorruption]

    def customerOriginatorOrBeneficiary(transaction: TransactionWithCustomerIds): Option[String] = {

      transaction.debitCredit match {
        case "D" => Some("originator")
        case "C" => Some("beneficiary")
        case _ => None
      }
    }

    def getCustomerName(transaction: TransactionWithCustomerIds): Option[String] = {
      customerOriginatorOrBeneficiary(transaction).flatMap(_ match {
        case "originator" => transaction.originatingAccount.flatMap(_.accountName)
        case "beneficiary" => transaction.beneficiaryAccount.flatMap(_.accountName)
        case _ => None
      })
    }

    def customerAccountCountry(transaction: TransactionWithCustomerIds): Option[String] = {
      customerOriginatorOrBeneficiary(transaction).flatMap(_ match {
        case "originator" => transaction.originatingAccount.flatMap(_.accountCountryISO3Code)
        case "beneficiary" => transaction.beneficiaryAccount.flatMap(_.accountCountryISO3Code)
        case _ => None
      })
    }

    def getCustomerAccount(transaction: TransactionWithCustomerIds): Option[TransactionAccount] = {
      customerOriginatorOrBeneficiary(transaction).flatMap(_ match {
        case "originator" => Some(transaction.originatingAccount).flatten
        case "beneficiary" => Some(transaction.beneficiaryAccount).flatten
        case _ => None
      }) //CustomerAccount should always be known, however it may not be as data is auto generated (and debit/credit may not properly align)
    }


    def customerAccountCountryCorruption(transaction: TransactionWithCustomerIds, origCtryCorruption: Option[Int], beneCtryCorruption: Option[Int]): Option[Int] = {
      customerOriginatorOrBeneficiary(transaction).flatMap(_ match {
        case "originator" => origCtryCorruption
        case "beneficiary" => beneCtryCorruption
        case _ => None
      })
    }

    def counterPartyAccountCountry(transaction: TransactionWithCustomerIds): Option[String] = {
      customerOriginatorOrBeneficiary(transaction).flatMap(_ match {
        case "beneficiary" => transaction.originatingAccount.flatMap(_.accountCountryISO3Code)
        case "originator" => transaction.beneficiaryAccount.flatMap(_.accountCountryISO3Code)
        case _ => None
      })
    }

    def getCounterpartyAccount(transaction: TransactionWithCustomerIds): Option[TransactionAccount] = {
      customerOriginatorOrBeneficiary(transaction).flatMap(_ match {
        case "beneficiary" => transaction.originatingAccount
        case "originator" => transaction.beneficiaryAccount
        case _ => None
      })
    }

    def counterPartyAccountCountryCorruption(transaction: TransactionWithCustomerIds, origCtryCorruption: Option[Int], beneCtryCorruption: Option[Int]): Option[Int] = {
      customerOriginatorOrBeneficiary(transaction).flatMap(_ match {
        case "beneficiary" => origCtryCorruption
        case "originator" => beneCtryCorruption
        case _ => None
      })
    }

    def getAnalysisAmount(transaction: TransactionWithCustomerIds) = {
      val amount = transaction.txnAmountInBaseCurrency
      if (amount >= 0) amount else amount * -1
    }

    val scoreableTransactions = transactionWithCustomerIds.map(txn =>
      ScoreableTransaction(
        transactionId = txn.transactionId,
        customerId = txn.customerId,
        customerName = getCustomerName(txn),
        debitCredit = txn.debitCredit,
        customerOriginatorOrBeneficiary = customerOriginatorOrBeneficiary(txn),
        postingDate = new java.sql.Date(txn.postingDate.getTime),
        analysisDate = new java.sql.Date(txn.postingDate.getTime),
        monthsSinceEpoch = monthsSinceEpoch(txn.runDate.toLocalDate),
        amountOrigCurrency = txn.txnAmount,
        origCurrency = txn.txnCurrency,
        amountInGBP = txn.txnAmountInBaseCurrency,
        analysisAmount = getAnalysisAmount(txn),
        customerAccount = getCustomerAccount(txn).get,
        counterpartyAccount = getCounterpartyAccount(txn),
        customerAccountCountry = customerAccountCountry(txn),
        customerAccountCountryCorruptionIndex = None,
        counterPartyAccountCountry = counterPartyAccountCountry(txn),
        counterPartyAccountCountryCorruptionIndex = None,
        description = txn.txnDescription,
        counterpartyAccountId = txn.counterpartyAccountId,
        counterpartyCustomerId = txn.counterpartyCustomerId,
        paymentToCustomersOwnAccountAtBank =  Some(txn.customerId == txn.counterpartyCustomerId.getOrElse("")))
    )

    def joinScoreableTransactionsWithCountryCorruption(scorableTransactionDS: Dataset[ScoreableTransaction],
                                                       countryCorruptionDF: Dataset[CountryCorruption], joinField: String) = {

      scorableTransactionDS.joinWith(countryCorruptionDF, scorableTransactionDS(joinField) <=> countryCorruptionDF("ISO3"), "left")
        .map {
          case (txn, null) => txn
          case (txn, countryCorruption) =>
            txn.copy(
              customerAccountCountryCorruptionIndex = if (joinField == "customerAccountCountry") Some(countryCorruption.corruptionIndex) else txn.customerAccountCountryCorruptionIndex,
              counterPartyAccountCountryCorruptionIndex = if (joinField == "counterPartyAccountCountry") Some(countryCorruption.corruptionIndex) else txn.counterPartyAccountCountryCorruptionIndex
            )
        }
    }

    val scoreableTransactionsAndCorruption = joinScoreableTransactionsWithCountryCorruption(scoreableTransactions, countryCorruption, "customerAccountCountry")
    joinScoreableTransactionsWithCountryCorruption(scoreableTransactionsAndCorruption, countryCorruption1, "counterPartyAccountCountry")
  }

  def monthsSinceEpoch(date: java.time.LocalDate): Long = {
    val epochDay = java.time.LocalDate.ofEpochDay(0)
    ChronoUnit.MONTHS.between(epochDay, date)
  }

  def monthsSinceEpochToEndOfCalendarMonth(monthsSinceEpoch: Long): java.time.LocalDate = {
    ChronoUnit.MONTHS.addTo(java.time.LocalDate.ofEpochDay(0), monthsSinceEpoch + 1)
  }

  def lastCompleteMonthSinceEpoch: Long = {
    val configDateAsLocalDate = config.runDateDt.toLocalDate
    val runDateIsOnEndOfMonth = configDateAsLocalDate.getDayOfMonth == configDateAsLocalDate.lengthOfMonth
    if(runDateIsOnEndOfMonth)
      monthsSinceEpoch(configDateAsLocalDate)
    else
      monthsSinceEpoch(configDateAsLocalDate) - 1
  }

}