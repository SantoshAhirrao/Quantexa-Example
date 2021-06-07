package com.quantexa.example.scoring.batch.scores.fiu.facts

import frameless.Job
import com.github.mrpowers.spark.daria.sql.DataFrameHelpers
import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.scoring.batch.model.fiu.TransactionInputCustomScore
import com.quantexa.example.scoring.batch.scores.fiu.facts.aggregatedtransaction.{AggregatedTransactionMiscAggregations, AggregatedTransactionValueStats, AggregatedTransactionVolumeStats}
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel
import org.apache.spark.sql.{Column, DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.functions._
import com.quantexa.example.scoring.batch.utils.facts.Utils._
import com.quantexa.example.scoring.model.fiu.FactsModel.AggregatedTransactionFacts

import scala.collection.immutable.ListMap

case class CalculateAggregatedTransactionFacts(config: ProjectExampleConfig,
                                               @transient aggregatedTransactionStats: AggregatedTransactionConfiguration =
                                               AggregatedTransactionValueStats.configuration + AggregatedTransactionVolumeStats.configuration + AggregatedTransactionMiscAggregations.configuration
                                              ) extends TransactionInputCustomScore {

  override def id: String = "AggregationFacts"

  override def processScoreableTransaction(spark: SparkSession,
                                           transactions: Dataset[ScoreableTransaction])(implicit scoreInput: ScoreModel.ScoreInput): Unit = {

    val output = calculateAggregatedTransactionFacts(transactions, aggregatedTransactionStats).run()

    output.write.mode("overwrite").parquet(config.hdfsFolderAggregatedTransactionFacts)
  }

  private def calculateAggregatedTransactionFacts(transactions: Dataset[ScoreableTransaction],
                                                  aggregatedTransactionStats: AggregatedTransactionConfiguration): Job[Dataset[AggregatedTransactionFacts]] =
    (for {
      calculatedStats <- calculateAggregatedTransactionFactsStats(transactions, aggregatedTransactionStats)
      aggregatedTransactionFacts <- aggregatedTransactionFactsToDataset(calculatedStats)
    } yield aggregatedTransactionFacts).withDescription("Calculates aggregated transaction statistics, including rolling statistics, from transactions, returning a Dataset[AggregatedTransactionFacts]")


  private def aggregatedTransactionFactsToDataset(df: DataFrame): Job[Dataset[AggregatedTransactionFacts]] = Job(
    {
      import df.sparkSession.implicits._
      toDataset[AggregatedTransactionFacts](df)
    }
  )(df.sparkSession)


  private def calculateAggregatedTransactionFactsStats(transactions: Dataset[ScoreableTransaction],
                                                       aggregatedTransactionStats: AggregatedTransactionConfiguration): Job[DataFrame] = Job(
    calculateAllStats(transactions, aggregatedTransactionStats)
  )(transactions.sparkSession).withDescription(
    "Calculates statistics based on a scoreable transaction"
  )

  private def calculateAllStats(transactions: Dataset[ScoreableTransaction],
                                aggregatedTransactionStats: AggregatedTransactionConfiguration): DataFrame = {

    val maxPostingDate = getMaxPostingDate(transactions)
    val addedMaxPostingDate = transactions.toDF().withColumn("maxDate", lit(maxPostingDate))

    val addedStats = addStats(addedMaxPostingDate, aggregatedTransactionStats.stats)

    DataFrameHelpers.validatePresenceOfColumns(addedStats, Seq("customerId", "counterpartyAccountId") ++ aggregatedTransactionStats.stats.keySet)

    val addedMissingStatistics = fillMissingStatistics(addedStats, aggregatedTransactionStats.stats.keySet.toSeq)

    makeKeyColumn(addedMissingStatistics)
  }

  private def fillMissingStatistics(dataFrame: DataFrame, columns: Seq[String]): DataFrame = {
    dataFrame.na.fill(0, columns)
  }

  private def addStats(transactions: DataFrame,
                       stats: collection.Map[String, Column]): DataFrame = {

    val statsSeq = stats.map {
      case (name, stat) => stat.as(name)
    }.toSeq

    statsSeq.size match {
      case 0 => throw new IllegalArgumentException("Please specify at least one statistic to calculate at aggregate transaction level")
      case _ => transactions.groupBy("customerId", "counterpartyAccountId").agg(statsSeq.head, statsSeq.tail: _*)
    }
  }

  private def getMaxPostingDate(transactions: Dataset[ScoreableTransaction]): java.sql.Date = {
    import transactions.sparkSession.implicits._
    val maxPostingDate = transactions.select(max(col("postingDate"))).as[java.sql.Date].collect.apply(0)

    maxPostingDate
  }

  private def makeKeyColumn(df: DataFrame): DataFrame = {
    def udfKeyString = udf((customerId: String, counterpartyAccountId: String) => customerId.concat("|").concat(counterpartyAccountId))

    df.withColumn("aggregatedTransactionId", udfKeyString(col("customerId"), col("counterpartyAccountId")))
  }
}

object CalculateAggregatedTransactionFacts{

  def filterByNumberMonths(dateToFilter:Column, filterRelativeToDate: Column, numMonths: Int, columnOfInterest: Column): Column =
    when(dateToFilter.geq(add_months(filterRelativeToDate, -numMonths)).and(dateToFilter.leq(filterRelativeToDate)), columnOfInterest).otherwise(null)

  def filterByNumberMonths(dateToFilter:Column, filterRelativeToDate: Column, numMonths:Int): Column =
    filterByNumberMonths(dateToFilter, filterRelativeToDate, numMonths, dateToFilter)

}


case class AggregatedTransactionConfiguration(stats: ListMap[String, Column]) {

  /**
    * Filters calculated stats by name
    */
  def filter(predicate: String => Boolean): AggregatedTransactionConfiguration = {
    AggregatedTransactionConfiguration(
      stats = stats.filter(x => predicate(x._1))
    )
  }

  def +(a: AggregatedTransactionConfiguration): AggregatedTransactionConfiguration = {

    errorIfSharedKeysHaveDifferentValues(this.stats, a.stats)

    AggregatedTransactionConfiguration(stats = this.stats ++ a.stats)
  }

  private def errorIfSharedKeysHaveDifferentValues(map1: Map[String, Column], map2: Map[String, Column]): Unit = {
    val sharedKeys = map1.keys.toSeq.intersect(map2.keys.toSeq)
    if (sharedKeys.nonEmpty) {
      sharedKeys.foreach(
        k => {
          val (calculation1, calculation2) = (map1(k), map2(k))
          if (calculation1 != calculation2) throw new IllegalArgumentException(
            s"The field $k has  two different calculations specified. These are $calculation1 and $calculation2. Please check for duplication across all scores being ran"
          )
        }
      )
    }
  }
}

object AggregatedTransactionConfiguration {
  def empty = AggregatedTransactionConfiguration(ListMap.empty)
}

