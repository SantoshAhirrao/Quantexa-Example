package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate

import com.quantexa.analytics.spark.sql.functions.CategoryCounter
import com.quantexa.example.scoring.batch.model.fiu.CustomerPreviousCountriesStats
import com.quantexa.example.scoring.batch.utils.fiu.CustomerDateFactsTestSuite
import com.quantexa.example.scoring.batch.utils.fiu.excel.ExcelUtils

import collection.JavaConverters._
import com.quantexa.example.scoring.model.fiu.ScoringModel.CustomerDateKey
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{IntegerScoreParameter, ParameterIdentifier}
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.{SparkConf, SparkContext}
import org.joda.time.{DateTimeZone, LocalDate}
import monocle.Lens
import monocle.macros.GenLens
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
  * Supporting Data for these tests can be found in confluence at
  * https://quantexa.atlassian.net/wiki/spaces/TECH/pages/910262689/Customer+Date+Facts+Tests
  * This confluence page must be updated where changes are made to the test data.
  *com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate.RollingCustomerPrevCountriesTest
  */
@RunWith(classOf[JUnitRunner])
class RollingCustomerPrevCountriesTest extends CustomerDateFactsTestSuite {

  import RollingCustomerPrevCountriesTest._
  import PreviousCountriesTestData._

  override def sc: SparkContext = spark.sparkContext

  override def conf: SparkConf = spark.sparkContext.getConf

  import spark.implicits._

  implicit def mapIntIntEncoder: Encoder[Map[String, Long]] = ExpressionEncoder()

  override def statisticsConfiguration = RollingCustomerPrevCountries.configuration

  it should "add 2 new columns and calculate them correctly based on the input of RollingValueVolume score for a single customer" in {
    val singleCustomerId = "A"
    checkStatsForCustomer(
      singleCustomerId,
      customerATransactions.toDS(),
      statisticsConfiguration,
      _expectedDataset
      )(implicitly, implicitly, ScoreInput.empty.copy(
      parameters = Map(
        ParameterIdentifier(None, "PastNumberOfMonthsForFactAggregation") -> IntegerScoreParameter("PastNumberOfMonthsForFactAggregation", "test", 24)
        )
      )
    )
  }

  it should "add 2 new columns and calculate them correctly based on the input of RollingValueVolume score for multiple customers" in {
    val customerBTransactions = customerATransactions.map(_.copy(customerId = "B"))
    val lens: Lens[CustomerPreviousCountriesStats, String]= GenLens[CustomerPreviousCountriesStats](_.key.customerId)
    val multipleCustomerTransactionDS = scala.util.Random.shuffle(customerATransactions ++ customerBTransactions).toDS
    checkStatsForCustomer("A", multipleCustomerTransactionDS, statisticsConfiguration,  _expectedDataset)(implicitly, implicitly, ScoreInput.empty.copy(
      parameters = Map(
        ParameterIdentifier(None, "PastNumberOfMonthsForFactAggregation") -> IntegerScoreParameter("PastNumberOfMonthsForFactAggregation", "test", 24)))
    )
    checkStatsForCustomer("B", multipleCustomerTransactionDS, statisticsConfiguration,  _expectedDataset.map(lens.modify(Function.const("B"))(_)))(implicitly, implicitly, ScoreInput.empty.copy(parameters = Map(
        ParameterIdentifier(None, "PastNumberOfMonthsForFactAggregation") -> IntegerScoreParameter("PastNumberOfMonthsForFactAggregation", "test", 24)))
    )
  }
}

object RollingCustomerPrevCountriesTest {
  implicit def localDateToSqlDate(s: String): java.sql.Date = new java.sql.Date(
    LocalDate.parse(s).toDateTimeAtStartOfDay(jodaTzUTC).getMillis)

  val jodaTzUTC = DateTimeZone.forID("UTC")

  import ExcelUtils._
  import com.quantexa.example.scoring.batch.utils.fiu.CustomerDateFactsTestData._

  val rawDataWorkbookSheet = workbook.getSheet("Countries Stats")
  val rows = rawDataWorkbookSheet.iterator().asScala.toSeq.drop(2)  //To filter the header

  val _expectedDataset = rows.filter(row => filterRowsBySpecificNotEmptyCell(row, 8)).map(row =>
    CustomerPreviousCountriesStats(CustomerDateKey(row.getCell(0).asString,row.getCell(1).asDateString),
      null,
      CategoryCounter(row.getCell(8).asMap),
      CategoryCounter(row.getCell(9).asMap),
      CategoryCounter(row.getCell(10).asMap),
      CategoryCounter(row.getCell(11).asMap),
      row.getCell(16).asLong,
      row.getCell(17).asDouble)
  )
}