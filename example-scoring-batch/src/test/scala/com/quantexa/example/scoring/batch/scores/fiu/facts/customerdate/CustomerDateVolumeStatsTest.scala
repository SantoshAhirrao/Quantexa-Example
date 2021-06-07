package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate

import com.quantexa.analytics.scala.Utils.getFieldNames
import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.scoring.batch.utils.fiu.{CustomerDateFactsTestSuite, CustomerTransactionCountStats}
import com.quantexa.example.scoring.batch.utils.fiu.excel.ExcelUtils
import com.quantexa.example.scoring.model.fiu.ScoringModel.CustomerDateKey
import collection.JavaConverters._
import org.apache.spark.{SparkConf, SparkContext}
import org.joda.time.{DateTimeZone, LocalDate}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
  * Supporting Data for these tests can be found in confluence under the test section of https://quantexa.atlassian.net/wiki/spaces/PE/pages/979075438/5.+Scoring
  * This confluence page must be updated where changes are made to the test data.
  */
@RunWith(classOf[JUnitRunner])
class CustomerDateVolumeStatsTest extends CustomerDateFactsTestSuite {

  import CustomerDateVolumeStatsTest._

  override def sc: SparkContext = spark.sparkContext

  override def conf: SparkConf = spark.sparkContext.getConf

  import spark.implicits._

  override def statisticsConfiguration = TransactionVolumeStats.configuration

  "Test Data Model for TransactionValueStdDevStats" should "contain all fields in configuration of TransactionValueStdDevStats" in {
    val actualFieldsInTestData = getFieldNames[CustomerTransactionCountStats].toSet
    actualFieldsInTestData shouldEqual expectedOutputFields
  }

  "CustomerDate TransactionVolumeStats" should "correctly calculate statistics for a single customer" in {
    val singleCustomerId = "A"

    assertExpectedStatsEqualToActualStats(singleCustomerId, expectedCustomerAOutput.toDS, singleCustomerActualOutput, dateRangesToCheck)
  }

  it should "correctly calculate statistics for multiple customers" in {

    assertExpectedStatsEqualToActualStats("A", expectedCustomerAOutput.toDS, twoCustomerActualOutput, dateRangesToCheck)
    assertExpectedStatsEqualToActualStats("B", expectedCustomerBOutput.toDS, twoCustomerActualOutput, dateRangesToCheck)
  }

}

object CustomerDateVolumeStatsTest {
  implicit def localDateToSqlDate(s: String): java.sql.Date =
    new java.sql.Date(LocalDate.parse(s).toDateTimeAtStartOfDay(jodaTzUTC).getMillis)

  val jodaTzUTC = DateTimeZone.forID("UTC")

  def toScoreableTransaction(_customerId: String, _analysisAmount: Double, _transactionDate: String) =
    ScoreableTransaction.empty.copy(customerId = _customerId, analysisAmount = _analysisAmount, analysisDate = java.sql.Date.valueOf(_transactionDate))

  val dateRangesToCheck = Map("2017-11-08" -> "2017-11-16", "2018-01-05" -> "2018-01-06")

  import ExcelUtils._
  import com.quantexa.example.scoring.batch.utils.fiu.CustomerDateFactsTestData._

  val rawDataWorkbookSheet = workbook.getSheet("Volume Stats")
  val rows = rawDataWorkbookSheet.iterator().asScala.toSeq.tail //To filter the header

  val dateRangesToCheckInDateFormat = dateRangesStringToDateFormat(dateRangesToCheck)

  val expectedCustomerAOutput = rows.filter(row => filterByDate(row, dateRangesToCheckInDateFormat, 1))
    .sortWith {
      case (leftRow, rightRow) => rowsSorted(leftRow, rightRow, 1)
    }.map { row =>
      CustomerTransactionCountStats(CustomerDateKey(row.getCell(0).asString, row.getCell(1).asDateString),
        row.getCell(2).asLong,
        row.getCell(3).asLong,
        row.getCell(4).asLong,
        row.getCell(5).asLong,
        row.getCell(6).asLong,
        row.getCell(7).asOptionDouble,
        row.getCell(8).asOptionDouble,
        row.getCell(9).asOptionDouble,
        row.getCell(10).asOptionDouble,
        row.getCell(11).asOptionDouble,
        row.getCell(12).asOptionDouble)
  }

  val expectedCustomerBOutput = expectedCustomerAOutput.map(x => x.copy(key = x.key.copy(customerId = "B")))
}


