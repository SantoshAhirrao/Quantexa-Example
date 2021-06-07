package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate

import com.quantexa.analytics.scala.Utils.getFieldNames
import com.quantexa.example.scoring.batch.utils.fiu.{CustomerDateFactsTestSuite, CustomerTransactionValueStats}
import com.quantexa.example.scoring.model.fiu.ScoringModel.CustomerDateKey
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
class CustomerDateValueStatsTest extends CustomerDateFactsTestSuite {

  import CustomerDateValueStatsTest._

  override def sc: SparkContext = spark.sparkContext

  override def conf: SparkConf = spark.sparkContext.getConf

  import spark.implicits._

  override def statisticsConfiguration = TransactionValueStats.configuration

  "Test Data Model for TransactionValueStdDevStats" should "contain all fields in configuration of TransactionValueStdDevStats" in {
    val actualFieldsInTestData = getFieldNames[CustomerTransactionValueStats].toSet
    actualFieldsInTestData shouldEqual expectedOutputFields
  }

  "CustomerDate TransactionValueStats" should "correctly calculate statistics for a single customer" in {
    assertExpectedStatsEqualToActualStats("A", expectedCustomerAOutput.toDS, singleCustomerActualOutput, dateRangesToCheck)
  }

  singleCustomerActualOutput.unpersist()

  it should "correctly calculate statistics for multiple customers" in {
    assertExpectedStatsEqualToActualStats("A", expectedCustomerAOutput.toDS, twoCustomerActualOutput, dateRangesToCheck)
    assertExpectedStatsEqualToActualStats("B", expectedCustomerBOutput.toDS, twoCustomerActualOutput, dateRangesToCheck)
  }

}

object CustomerDateValueStatsTest {
  implicit def localDateToSqlDate(s: String): java.sql.Date =
    new java.sql.Date(LocalDate.parse(s).toDateTimeAtStartOfDay(jodaTzUTC).getMillis())

  val jodaTzUTC = DateTimeZone.forID("UTC")

  val dateRangesToCheck = Map("2017-11-08" -> "2017-11-16", "2018-01-05" -> "2018-01-07")

  import com.quantexa.example.scoring.batch.utils.fiu.excel.ExcelUtils._
  import com.quantexa.example.scoring.batch.utils.fiu.CustomerDateFactsTestData._

  val rawDataWorkbookSheet = workbook.getSheet("Value Stats")
  val rows = rawDataWorkbookSheet.iterator().asScala.toSeq.tail  //To filter the header

  val dateRangesToCheckInDateFormat = dateRangesStringToDateFormat(dateRangesToCheck)

  val expectedCustomerAOutput =  rows.filter(row => filterByDate(row, dateRangesToCheckInDateFormat, 1))
    .sortWith{
      case (leftRow, rightRow) => rowsSorted(leftRow, rightRow, 1)
    }.map { row =>
    CustomerTransactionValueStats(CustomerDateKey(row.getCell(0).asString, row.getCell(1).asDateString),
      row.getCell(2).asDouble,
      row.getCell(3).asDouble,
      row.getCell(4).asDouble,
      row.getCell(5).asDouble,
      row.getCell(6).asDouble,
      row.getCell(7).asOptionDouble,
      row.getCell(8).asOptionDouble,
      row.getCell(9).asOptionDouble,
      row.getCell(10).asOptionDouble,
      row.getCell(11).asOptionDouble,
      row.getCell(12).asOptionDouble)
  }

  val expectedCustomerBOutput = expectedCustomerAOutput.map(x => x.copy(key = x.key.copy(customerId = "B")))
}

