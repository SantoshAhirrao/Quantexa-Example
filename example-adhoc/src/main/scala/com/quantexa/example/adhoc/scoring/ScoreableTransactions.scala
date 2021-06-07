package com.quantexa.example.adhoc.scoring

import org.apache.spark.sql.{Encoder, Encoders, SparkSession}

/* The contents of this class are used purely to paste in Spark Shell to get a Dataset[ScoreableTranasaction]
 * Since this is ad-hoc, delete if it causes any issues */
class ScoreableTransactions(spark: SparkSession) {

  import org.apache.spark.sql.types.{DataType, MapType, StructField, StructType}
  import com.quantexa.example.model.fiu.transaction.TransactionModel.{Transaction, TransactionAccount}
  import com.quantexa.example.model.fiu.transaction.scoring.{ScoreableTransaction,TransactionWithCustomerIds}
  import com.quantexa.example.scoring.model.fiu.ScoringModel.CountryCorruption
  import com.quantexa.scriptrunner.util.DevelopmentConventions.FolderConventions
  import org.apache.spark.sql.functions._
  import org.apache.spark.sql.{Column, Dataset, DataFrame}
  import spark.implicits._
  import com.quantexa.example.model.fiu.utils.DateModel.generateDateRange
  import com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate.{RollingCustomerPrevCountries, TransactionValueStats, TransactionValueStdDevStats, TransactionVolumeStats}
  import com.quantexa.example.scoring.model.fiu.ScoringModel.CustomerDateKey
  import com.quantexa.analytics.scala.Utils.getFieldNames
  import scala.reflect.runtime.universe.TypeTag
  import com.quantexa.example.scoring.model.fiu.FactsModel.CustomerDateFacts
  import java.time.temporal.ChronoUnit
  import scala.annotation.tailrec

  def monthsSinceEpoch(date:java.time.LocalDate): Long = {
    val monthsSinceEpoch = java.time.LocalDate.ofEpochDay(0)
    ChronoUnit.MONTHS.between(monthsSinceEpoch, date)
  }

  val transactionPath = FolderConventions.cleansedCaseClassFile("/user/aaronlee/transaction/")
  val transactions = spark.read.parquet(transactionPath).as[Transaction]

  def createScoreableTransaction(ds: Dataset[Transaction]): Dataset[ScoreableTransaction] = {

    val spark = ds.sparkSession
    import spark.implicits._

    val customerCleansed = spark.read.parquet("/user/aaronlee/customer/DocumentDataModel/CleansedDocumentDataModel.parquet")
    val customerAccountIds = customerCleansed.select($"customerIdNumberString", explode($"accounts").as("account"))
      .select($"customerIdNumberString".as("customerId"), $"account.primaryAccountNumber".as("accountId"))

    /* this is necessary to avoid the spark trivally true bug that misintereprets self joined tables as cartesian products
   https://stackoverflow.com/questions/32190828/spark-sql-performing-carthesian-join-instead-of-inner-join */

    val customerCleansed2 = spark.read.parquet("/user/aaronlee/customer/DocumentDataModel/CleansedDocumentDataModel.parquet")
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


    val countryCorruption = spark.read.parquet(s"/user/aaronlee/scoring/raw/parquet/CorruptionIndex.parquet").as[CountryCorruption]
    /* this is necessary to avoid the spark trivally true bug that misintereprets self joined tables as cartesian products
       https://stackoverflow.com/questions/32190828/spark-sql-performing-carthesian-join-instead-of-inner-join */
    val countryCorruption1 = spark.read.parquet(s"/user/aaronlee/scoring/raw/parquet/CorruptionIndex.parquet").as[CountryCorruption]

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

    def getCustomerAccount(transaction: TransactionWithCustomerIds): TransactionAccount = {
      customerOriginatorOrBeneficiary(transaction).flatMap(_ match {
        case "originator" => transaction.originatingAccount
        case "beneficiary" => transaction.beneficiaryAccount
        case _ => None
      }).get //CustomerAccount should always be known
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
        monthsSinceEpoch =  monthsSinceEpoch(txn.runDate.toLocalDate),
        amountOrigCurrency = txn.txnAmount,
        origCurrency = txn.txnCurrency,
        amountInGBP = txn.txnAmountInBaseCurrency,
        analysisAmount = getAnalysisAmount(txn),
        customerAccount = getCustomerAccount(txn),
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

  val scoreableTransactions = createScoreableTransaction(transactions)

  def dateColumn: String = "analysisDate"

  val datesBetween = udf { (minDate: java.sql.Date, maxDate: java.sql.Date) =>
    generateDateRange(minDate, maxDate)
  }

  def addSimpleAggregations(transactions: Dataset[ScoreableTransaction],
                            aggregations: collection.Map[String, Column]) = {
    val customerDateLevelStats2 = aggregations.map {
      case (name, stat) => stat.as(name)
    }.toSeq

    // TODO: Implement logic that takes the most frequent name listed on a customer transaction for each date

    val additionalAggregationColumns = Seq(max("customerName").as("customerName" ))

    val statsWithAdditionalColumns = additionalAggregationColumns ++ customerDateLevelStats2

    aggregations.size match {
      case 0 => throw new IllegalArgumentException("Please specify at least one statistic to calculate at customer date level")
      case _ => transactions.groupBy("customerId", dateColumn).agg(statsWithAdditionalColumns.head, statsWithAdditionalColumns.tail: _*)
    }
  }

  val stats = TransactionVolumeStats.configuration + TransactionValueStats.configuration + TransactionValueStdDevStats.configuration + RollingCustomerPrevCountries.configuration

  val simpleAggregateStats = addSimpleAggregations(scoreableTransactions, stats.customerDateLevelStats)

  def fillDateGaps(transactions: Dataset[ScoreableTransaction], simpleAggregateStats: DataFrame) = {

    val customersFirstTransactionDate = transactions.groupBy("customerId").agg(min(dateColumn).as("minDate"))

    implicit val dateEnc = org.apache.spark.sql.Encoders.DATE

    val maxDate = transactions.select(max(col(dateColumn))).as[java.sql.Date](dateEnc).collect.apply(0)
    val customersWithMaxDate = customersFirstTransactionDate.withColumn("maxDateOfAllTransactions", lit(maxDate))

    val customerDateWithNoGaps = customersWithMaxDate.withColumn(
      dateColumn,
      explode(datesBetween(col("minDate"), col("maxDateOfAllTransactions")))).
      select("customerId", dateColumn)

    customerDateWithNoGaps.join(simpleAggregateStats, Seq("customerId", dateColumn), "left")
  }

  val simpleAggregateStatsFilledDates1 = fillDateGaps(scoreableTransactions, simpleAggregateStats)

  def addLongDateTime(df: DataFrame): DataFrame = {
    def getUnixTimeUDF = udf((date: java.sql.Date) => date.getTime / 1000)

    df.withColumn("dateTime", getUnixTimeUDF(col(dateColumn)))
  }

  val simpleAggregateStatsFilledDates2 = addLongDateTime(simpleAggregateStatsFilledDates1)

  def fillMissingStatistics(df: DataFrame): DataFrame = {

    val statsColumns = df.schema.filterNot {
      case StructField(name, dataType, nullable, metadata) => Set("customerId", dateColumn)(name)
    }.map(x => (x.name, x.dataType))

    def fillData(data: DataFrame, columnName: String, default: Column): DataFrame = {
      data.withColumn(s"${columnName}Tmp", coalesce(col(columnName), default)).drop(columnName).withColumnRenamed(s"${columnName}Tmp", columnName)
    }
    type StatsColumn = (String, DataType)

    val filledData = statsColumns.foldLeft(df) {
      (data: DataFrame, statsColumn : StatsColumn) =>
        statsColumn match {
          // this captures the structure for generic category counter aggregation
          case (columnName, StructType(Array(StructField(_,MapType(keyType,valueType,_),_,_)))) =>
            fillData(data, columnName,
              struct(typedLit(Map.empty[String,Long]).as("holder")))
          // this provides the default Map[String,Long].empty for Maps
          case (columnName, MapType(_, _, _)) =>
            fillData(data, columnName, typedLit(Map.empty[String,Long]))
          //this provides the defauls 0 for numeric values (catch all).
          case (columnName, _ ) =>
            fillData(data, columnName, lit(0))}
    }
    filledData
  }

  val simpleAggregateStatsFilledDates3 = fillMissingStatistics(simpleAggregateStatsFilledDates2)

  def addRollingStatistics(df: DataFrame, rollingStats: Map[String, Column]): DataFrame = {
    rollingStats.foldLeft(df) {
      case (outDf, (colName, stat)) => {
        outDf.withColumn(colName, stat)
      }
    }
  }

  val complexAggregateStats = addRollingStatistics(simpleAggregateStatsFilledDates3, stats.rollingStats)

  def makeKeyColumn(df: DataFrame): DataFrame = {
    def udfKey = udf((cust: String, date: java.sql.Date) => CustomerDateKey(cust, date))
    def udfString = udf((cust: String, date: java.sql.Date) => CustomerDateKey.createStringKey(cust, date))
    df.withColumn("key", udfKey(col("customerId"), col(dateColumn))).
      withColumn("keyAsString", udfString(col("customerId"), col(dateColumn)))
  }

  val complexAggregateStatsWithKey = makeKeyColumn(complexAggregateStats)

  @tailrec
  final def setColumnOrderAndNullability(desiredOrderingAndNullability: Seq[StructField],
                                         unorderedCols: Seq[StructField],
                                         orderedOutputCols: Seq[StructField] = Seq.empty[StructField]): StructType = {
    desiredOrderingAndNullability.toList match {
      case Nil => StructType(orderedOutputCols ++ unorderedCols)
      case head :: tail => unorderedCols.map(x=>x.name).toSet(head.name) match {
        case true => setColumnOrderAndNullability(tail, unorderedCols.filter(_.name != head.name), orderedOutputCols :+ head)
        case false => setColumnOrderAndNullability(tail, unorderedCols, orderedOutputCols)
      }
    }
  }

  def setNullabilityAndColumnOrder[T <: Product : TypeTag](df: DataFrame): DataFrame = {
    val customerDateFactSchema = Encoders.product[T].schema
    val correctedSchema = setColumnOrderAndNullability(customerDateFactSchema, df.schema.toSeq)

    val reorderSelectStatement = correctedSchema.map(x=>col(x.name))
    df.sparkSession.createDataFrame(df.select(reorderSelectStatement :_*).rdd, correctedSchema)
  }

  setNullabilityAndColumnOrder[CustomerDateFacts](complexAggregateStatsWithKey)


}
