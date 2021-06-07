package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate

import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.scoring.batch.utils.fiu.{CustomerDateFactsTestSuite, CustomerTransactionValueStdDev}
import com.quantexa.example.scoring.model.fiu.ScoringModel.CustomerDateKey
import com.quantexa.analytics.scala.Utils.getFieldNames
import com.quantexa.example.scoring.batch.scores.fiu.facts.CustomerDateStatsConfiguration
import com.quantexa.example.scoring.batch.utils.fiu.excel.ExcelUtils
import org.apache.spark.{SparkConf, SparkContext}
import org.joda.time.{DateTimeZone, LocalDate}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
/**
  * Supporting Data for these tests can be found in confluence under the test section of https://quantexa.atlassian.net/wiki/spaces/PE/pages/979075438/5.+Scoring
  * This confluence page must be updated where changes are made to the test data.
  */
class CustomerDateTxnValueStdDevStatsTest extends CustomerDateFactsTestSuite {

  import CustomerDateTxnValueStdDevStatsTest._

  override def sc: SparkContext = spark.sparkContext

  override def conf: SparkConf = spark.sparkContext.getConf

  import spark.implicits._

  override def statisticsConfiguration: CustomerDateStatsConfiguration = TransactionVolumeStats.configuration.filter(Set("numberOfTransactionsToday", "numberOfTransactionsToDate")(_)) +
    TransactionValueStats.configuration.filter(Set("totalValueTransactionsToday", "totalTransactionValueToDate")(_)) +
    TransactionValueStdDevStats.configuration

  val expectedOutputFieldsWithSMAEMA = (expectedOutputFields ++ Seq("customerDebitTransactionEMAOverLast30Days", "customerDebitTransactionSMAOverLast30Days")).toSet

  "Test Data Model for TransactionValueStdDevStats" should "contain all fields in configuration of TransactionValueStdDevStats" in {
    val actualFieldsInTestData = getFieldNames[CustomerTransactionValueStdDev].toSet
    actualFieldsInTestData shouldEqual expectedOutputFieldsWithSMAEMA
  }

  "CustomerDate TransactionValueStdDevStats Output" should "output fields for all stats which were calculated" in {
    singleCustomerActualOutput.columns.toSet shouldEqual expectedOutputFieldsWithSMAEMA
  }

  it should "correctly calculate statistics for a single customer" in {
    val singleCustomerId = "A"
    assertExpectedStatsEqualToActualStats(singleCustomerId, expectedCustomerAOutput.toDS, singleCustomerActualOutput, dateRangesToCheck)
  }

  singleCustomerActualOutput.unpersist()

  it should "correctly calculate statistics for multiple customers" in {
    assertExpectedStatsEqualToActualStats("A", expectedCustomerAOutput.toDS, twoCustomerActualOutput, dateRangesToCheck)
    assertExpectedStatsEqualToActualStats("B", expectedCustomerBOutput.toDS, twoCustomerActualOutput, dateRangesToCheck)
  }
  twoCustomerActualOutput.unpersist()

}

object CustomerDateTxnValueStdDevStatsTest {
  implicit def localDateToSqlDate(s: String): java.sql.Date =
    new java.sql.Date(LocalDate.parse(s).toDateTimeAtStartOfDay(jodaTzUTC).getMillis())

  val jodaTzUTC = DateTimeZone.forID("UTC")

  val dateRangesToCheck = Map("2017-11-08" -> "2017-11-16", "2018-01-05" -> "2018-01-07")

  def toScoreableTransaction(_customerId: String, _analysisAmount: Double, _transactionDate: String) =
    ScoreableTransaction.empty.copy(customerId = _customerId, analysisAmount = _analysisAmount, analysisDate = java.sql.Date.valueOf(_transactionDate))

  import ExcelUtils._
  import com.quantexa.example.scoring.batch.utils.fiu.CustomerDateFactsTestData._

  val rawDataWorkbookSheet = workbook.getSheet("StdDev Stats")
  val rows = rawDataWorkbookSheet.iterator().asScala.toSeq.tail //To filter the header

  val dateRangesToCheckInDateFormat = dateRangesStringToDateFormat(dateRangesToCheck)

  val expectedCustomerAOutput = rows.filter(row => filterByDate(row, dateRangesToCheckInDateFormat, 1))
    .sortWith {
      case (leftRow, rightRow) => rowsSorted(leftRow, rightRow, 1)
    }.map { row =>
      CustomerTransactionValueStdDev(CustomerDateKey(row.getCell(0).asString, row.getCell(1).asDateString),
        row.getCell(2).asLong,
        row.getCell(3).asLong,
        row.getCell(4).asDouble,
        row.getCell(5).asDouble,
        row.getCell(9).asOptionDouble,
        row.getCell(6).asDouble,
        row.getCell(7).asOptionDouble,
        row.getCell(8).asOptionDouble,
        row.getCell(12).asOptionDouble,
        row.getCell(13).asOptionDouble)
  }

  val expectedCustomerBOutput = expectedCustomerAOutput.map(x => x.copy(key = x.key.copy(customerId = "B")))
}




