package com.quantexa.example.scoring.batch.scores.fiu.facts

import frameless.Job
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.scoring.batch.model.fiu.TransactionInputCustomScore
import com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate.{RollingCustomerPrevCountries, TransactionValueStats, TransactionValueStdDevStats, TransactionVolumeStats}
import com.quantexa.example.scoring.model.fiu.FactsModel.{CustomerDateFacts,EMAFacts}
import com.quantexa.example.scoring.model.fiu.ScoringModel.CustomerDateKey
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.example.scoring.batch.utils.fiu.Utils.round
import org.apache.spark.sql.types.DoubleType
import scala.annotation.tailrec
import com.quantexa.scoring.framework.parameters._
import com.quantexa.example.model.fiu.utils.DateModel.generateDateRange
import scala.collection.immutable.ListMap

import com.quantexa.example.scoring.batch.utils.facts.Utils._

case class CalculateCustomerDateFacts(config: ProjectExampleConfig, @transient
                                      stats: CustomerDateStatsConfiguration =
      TransactionVolumeStats.configuration       +
      TransactionValueStats.configuration        +
      TransactionValueStdDevStats.configuration  +
      RollingCustomerPrevCountries.configuration) extends FactAggregation[CustomerDateFacts] with TransactionInputCustomScore with ScoreParameters {

  def id: String = "CustomerDateFacts"

  def dateColumn: String = "analysisDate"

  def parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(None, "PastNumberOfMonthsForFactAggregation"))

  def fillDateGaps(transactions: Dataset[ScoreableTransaction], simpleAggregateStats: DataFrame)(implicit scoreInput: ScoreInput): DataFrame = {

    val firstDateForTxnsOfInterest = java.sql.Date.valueOf(monthsSinceEpochToEndOfCalendarMonth(lastCompleteMonthSinceEpoch - parameter[Int]("PastNumberOfMonthsForFactAggregation")))

    val customersFirstTransactionDate = transactions.groupBy("customerId").agg(min(dateColumn).as("minDate"))

    import transactions.sparkSession.implicits._
    val maxDate = transactions.select(max(col(dateColumn))).as[java.sql.Date].collect.apply(0)
    val customersWithMaxDate = customersFirstTransactionDate.withColumn("maxDateOfAllTransactions", lit(maxDate)).as[(String, java.sql.Date, java.sql.Date)]

    val customerDateWithNoGaps = customersWithMaxDate.flatMap {
      case (customerId, minDate, maxDateOfAllTransactions) =>
        val minDateToUse = if (minDate before firstDateForTxnsOfInterest) firstDateForTxnsOfInterest else minDate
        generateDateRange(minDateToUse, maxDateOfAllTransactions).map(dt => (customerId, dt))
    }.withColumnRenamed("_1", "customerId")
      .withColumnRenamed("_2", dateColumn)

    customerDateWithNoGaps.join(simpleAggregateStats, Seq("customerId", dateColumn), "left")
  }

  def processScoreableTransaction(spark: SparkSession, transactions: Dataset[ScoreableTransaction])(implicit scoreInput: ScoreInput): Any = {
    val output = calculateCustomerDateFacts(transactions, stats).run()
    output.write.mode("overwrite").parquet(config.hdfsFolderCustomerDateFacts)
  }

  protected def makeKeyColumn(df: DataFrame): DataFrame = {
    def udfKey = udf((cust: String, date: java.sql.Date) => CustomerDateKey(cust, date))

    def udfString = udf((cust: String, date: java.sql.Date) => CustomerDateKey.createStringKey(cust, date))

    df.withColumn("key", udfKey(col("customerId"), col(dateColumn))).
      withColumn("keyAsString", udfString(col("customerId"), col(dateColumn)))
  }

  def calculateCustomerDateFactsStats(transactions: Dataset[ScoreableTransaction],
                                      stats: CustomerDateStatsConfiguration)(implicit scoreInput: ScoreInput): Job[DataFrame] = Job(
    {
      calculateAllStats(transactions, stats, addCustomStatistic)(scoreInput)
    }
  )(transactions.sparkSession).withDescription(
    "Calculates statistics based on a scoreable transaction and outputs one row per customer per date (including non transacting days)"
  )

  def customerDateFactsLikeToCustomerDateFacts(df: DataFrame): Job[Dataset[CustomerDateFacts]] = Job(
    {
      import df.sparkSession.implicits._
      toDataset[CustomerDateFacts](df)
    }
  )(df.sparkSession).withDescription("Selects columns for CustomerDateFacts and applies Dataset typing")

  def calculateCustomerDateFacts(transactions: Dataset[ScoreableTransaction],
                                 stats: CustomerDateStatsConfiguration)(implicit scoreInput: ScoreInput): Job[Dataset[CustomerDateFacts]] = (for {
    calculatedStats <- calculateCustomerDateFactsStats(transactions, stats)(scoreInput)
    custDateFacts <- customerDateFactsLikeToCustomerDateFacts(calculatedStats)
  } yield custDateFacts).withDescription("Calculates customer/date level statistics, including rolling statistics, from transactions, returning a Dataset[CustomerDateFacts]")

  /**
    * TODO: This is not configured in an ideal way, however it is difficult to add in arbitrary code whilst there is not strong dependency management.
    * How we would do this would certainly be revisited if there was a large amount of custom code, or the dependency management was improved.
    */
  protected def addCustomStatistic(df: DataFrame): DataFrame = {
    if (stats.customerDateLevelStats.keys.toSeq.contains("totalValueTransactionsToday")) {
      val emaFacts = extractEMAFacts(df)
      val emaStats = addEMA(emaFacts = emaFacts, smoothing = 5.0, daysInWindow = 30)
      df.join(emaStats, Seq("key"), "left")
    } else {
      df
    }
  }

   /**
     * @param daysInWindow - The number of days for the window. If a 10 day window is chosen, we work out SMA as the average of each day and it's 9 preceding values
     * @param customerDateFacts - Data to calculate SMA (simple moving average) for
     * @return Seq[CustomerDateFacts] where the first element is the first element that is eligible for an SMA value. So if days = 10, then we have customerDateFacts(9) onwards. The first 9 values get added back in later
     */
   def calculateSMA(daysInWindow: Int, customerDateFacts: Seq[EMAFacts]): List[EMAFacts] = {
     customerDateFacts.iterator.sliding(daysInWindow).map{
       window =>
         val row = window.last
         val sma = window.map(_.totalValueTransactionsToday).sum/daysInWindow.toDouble
         row.copy(customerDebitTransactionSMAOverLast30Days = Option(sma))
     }.toList
   }

   /**
     *
     * @param customerDateFacts A List[EMAFacts] which all belong to one customer. Note, this is a list because :: can only be used with list.
     * @param constantFactor Calculated previously as smoothingValue/(1 + daysInWindow)
     * @param results Seq to be returned upon completion
     * @param previousEMA Previous ema value to use in calculation of current ema value.
     * @return Seq[EMAFacts] with EMA (exponential moving average) values added for eligible instances. Formula can be found at https://www.investopedia.com/terms/e/ema.asp
     */
   @tailrec
   final def calculateEMARecursive(customerDateFacts: List[EMAFacts], constantFactor: Double, results: Seq[EMAFacts] = Seq.empty, previousEMA: Option[Double] = None): Seq[EMAFacts] = customerDateFacts match {
     case Nil => results
     case head :: Nil =>
       val ema = (head.totalValueTransactionsToday * constantFactor) + (previousEMA.getOrElse(head.customerDebitTransactionSMAOverLast30Days.get) * (1 - constantFactor))
       val roundedEma = round(ema, 4)
       results :+ head.copy(customerDebitTransactionEMAOverLast30Days = Option(roundedEma))
     case head :: rest =>
       val ema = (head.totalValueTransactionsToday * constantFactor) + (previousEMA.getOrElse(head.customerDebitTransactionSMAOverLast30Days.get) * (1 - constantFactor))
     val roundedEma = round(ema, 4)
       val row = head.copy(customerDebitTransactionEMAOverLast30Days = Option(roundedEma))
       calculateEMARecursive(rest, constantFactor, results :+ row, Option(roundedEma))
   }

   def extractEMAFacts(customerDateFacts: DataFrame): Dataset[EMAFacts] = {
     import customerDateFacts.sparkSession.implicits._
     customerDateFacts.
       select(
         $"key",
         $"totalValueTransactionsToday",
         lit(null).cast(DoubleType).as("customerDebitTransactionSMAOverLast30Days"),
         lit(null).cast(DoubleType).as("customerDebitTransactionEMAOverLast30Days")
       ).as[EMAFacts]
   }

   /**
     * @param emaFacts All customer date facts which have the transaction value and key field
     * @param smoothing Smoothing value for EMA formula
     * @param daysInWindow Size of window
     * @return All customer date facts with SMA and EMA values calculated where applicable. Output just has join key as well as sma and ema values
     */
   def addEMA (emaFacts: Dataset[EMAFacts], smoothing: Double, daysInWindow: Int) : DataFrame = {
     import emaFacts.sparkSession.implicits._

     emaFacts.groupByKey{emaFact  =>  emaFact.key.customerId}.mapGroups{
       case (customerId, customerDateFacts) =>

         val emaFactsForCustomer: Seq[EMAFacts] = customerDateFacts.toSeq.sortBy(_.key.analysisDate.getTime)

         val numberOfCustomerDateFacts = emaFactsForCustomer.length

         if (numberOfCustomerDateFacts < daysInWindow) {
           emaFactsForCustomer
         } else {
           val customerDateFactsWithSMA = calculateSMA(daysInWindow, emaFactsForCustomer)

           val formulaConstant = smoothing/(1+daysInWindow)

           emaFactsForCustomer.take(daysInWindow - 1) ++ calculateEMARecursive(customerDateFactsWithSMA, formulaConstant)
         }

     }.flatMap(identity).select(
       $"key",
       $"customerDebitTransactionSMAOverLast30Days",
       $"customerDebitTransactionEMAOverLast30Days")
   }

 }

/**
  * @param customerDateLevelStats Map of column name to Column Statistic.
  *                               Column statistic is applied after grouping by customerId and Date.
  * @param rollingStats Map of column name to Column Statistic.
  *                     Column statistic typically leverages the columns created by customerDateLevelStats.
  * Note immutable.ListMap used to maintain the insertion order, provides very limited support for dependency management.
  */
case class CustomerDateStatsConfiguration(customerDateLevelStats: ListMap[String, Column], rollingStats: ListMap[String, Column]) {

  errorOnSharedKeysBetweenSimpleAndRollingStats(customerDateLevelStats, rollingStats)

  /**
    * Filters calculated stats by name
    */
  def filter(predicate: String => Boolean): CustomerDateStatsConfiguration = {
    CustomerDateStatsConfiguration(
      customerDateLevelStats = customerDateLevelStats.filter(x => predicate(x._1)),
      rollingStats = rollingStats.filter(x => predicate(x._1))
    )
  }

  def +(a: CustomerDateStatsConfiguration): CustomerDateStatsConfiguration = {

    errorOnSharedKeysBetweenSimpleAndRollingStats(this.customerDateLevelStats, a.rollingStats)
    errorOnSharedKeysBetweenSimpleAndRollingStats(this.rollingStats, a.customerDateLevelStats)
    errorIfSharedKeysHaveDifferentValues(this.customerDateLevelStats, a.customerDateLevelStats)
    errorIfSharedKeysHaveDifferentValues(this.rollingStats, a.rollingStats)

    CustomerDateStatsConfiguration(this.customerDateLevelStats ++ a.customerDateLevelStats,
      this.rollingStats ++ a.rollingStats)
  }

  private def errorOnSharedKeysBetweenSimpleAndRollingStats(customerDateLevelStats: Map[String, Column],
                                                            rollingStats: Map[String, Column]): Unit = {
    val sharedKeys = customerDateLevelStats.keys.toSeq.intersect(rollingStats.keys.toSeq)
    if (sharedKeys.nonEmpty) throw new IllegalArgumentException(
      s"""Customer Date level stats must not contain the same field names as rolling stats. Duplicates field names are ${sharedKeys.mkString(",")}"""
    )
  }

  private def errorIfSharedKeysHaveDifferentValues(map1: Map[String, Column], map2: Map[String, Column]): Unit = {
    val sharedKeys = map1.keys.toSeq.intersect(map2.keys.toSeq)
    if (sharedKeys.nonEmpty) {
      sharedKeys.foreach(
        k => {
          val (calculation1, calculation2) = (map1(k), map2(k))
          if (map1(k) != map2(k)) throw new IllegalArgumentException(
            s"The field $k has two different calculations specified. These are $calculation1 and $calculation2. Please check for duplication across all scores being ran"
          )
        }
      )
    }
  }
}

object CustomerDateStatsConfiguration {
  def empty = CustomerDateStatsConfiguration(ListMap.empty, ListMap.empty)
}

