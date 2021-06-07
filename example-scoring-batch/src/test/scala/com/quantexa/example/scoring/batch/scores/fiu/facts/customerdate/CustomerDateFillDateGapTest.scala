package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate

import com.quantexa.example.scoring.batch.scores.fiu.facts.{CalculateCustomerMonthFacts, CustomerDateStatsConfiguration}
import com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate.ScoreableTransactionTestData.customerATransactions
import com.quantexa.example.scoring.batch.utils.fiu.CustomerDateFactsTestSuite
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{IntegerScoreParameter, ParameterIdentifier}
import org.apache.spark.{SparkConf, SparkContext}
import java.sql.Date

import org.apache.spark.sql.functions.col
import com.quantexa.example.scoring.batch.scores.fiu.facts.customermonth
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CustomerDateFillDateGapTest extends CustomerDateFactsTestSuite {

  def statisticsConfiguration : CustomerDateStatsConfiguration = TransactionValueStats.configuration

  override def sc: SparkContext = spark.sparkContext

  override def conf: SparkConf = spark.sparkContext.getConf

  import spark.implicits._

  val oneMonthLookbackSinceRunDate = custDateFacts.calculateCustomerDateFactsStats(
    customerATransactions.toDS(), statisticsConfiguration
  )(ScoreInput.empty.copy(
    parameters = Map(
      ParameterIdentifier(None, "PastNumberOfMonthsForFactAggregation") -> IntegerScoreParameter("PastNumberOfMonthsForFactAggregation", "test", 1))
  )).run().select(s"key.${custDateFacts.dateColumn}").as[java.sql.Date]

  "CalculateCustomerDateFacts fillDateGaps" should "not create empty days before the parameterised NumberOfMonthsLookbackPeriod if customers minDate is within the lookback period" in {
    val firstDate = oneMonthLookbackSinceRunDate.sort(col(custDateFacts.dateColumn).asc).take(1)

    assert(firstDate.length > 0)

    firstDate.head shouldEqual Date.valueOf("2017-12-01")
  }

  it should "not create any days before the parameterised NumberOfMonthsLookbackPeriod" in {
    oneMonthLookbackSinceRunDate.collect.contains(Date.valueOf("2017-11-25")) shouldEqual false
  }

  val customerMonthFacts = CalculateCustomerMonthFacts(
    config = ProjectExampleConfig.default("/").copy(runDate = "2018-01-08"),
    stats = customermonth.TransactionValueStats.configuration
  )

  val oneMonthLookbackForCustomerMonthFacts = customerMonthFacts.calculateCustomerMonthFactsStats(
    customerATransactions.toDS(), customermonth.TransactionValueStats.configuration
  )(ScoreInput.empty.copy(
    parameters = Map(
      ParameterIdentifier(None, "PastNumberOfMonthsForFactAggregation") -> IntegerScoreParameter("PastNumberOfMonthsForFactAggregation", "test", 1))
  )).run().select("key.calendarMonthEndDate").as[java.sql.Date]

  "CalculateCustomerMonthFacts fillDateGaps" should "not create empty months before the parameterised NumberOfMonthsLookbackPeriod if customers minDate is within the lookback period" in {
    val firstDate = oneMonthLookbackForCustomerMonthFacts.sort(col("key.calendarMonthEndDate").asc).take(1)

    assert(firstDate.length > 0)

    firstDate.head shouldEqual Date.valueOf("2017-12-01")
  }

  it should "not create any months before the parameterised NumberOfMonthsLookbackPeriod" in {
    oneMonthLookbackForCustomerMonthFacts.collect.contains(Date.valueOf("2017-11-01")) shouldEqual false
  }
}
