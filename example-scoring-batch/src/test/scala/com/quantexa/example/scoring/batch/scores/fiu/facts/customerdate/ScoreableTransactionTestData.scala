package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate

import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.scoring.batch.utils.fiu.excel.ExcelUtils
import org.joda.time.{DateTimeZone, LocalDate}

import collection.JavaConverters._

object ScoreableTransactionTestData {
  def toScoreableTransaction(_customerId: String, _analysisAmount: Double, _transactionDate: String) =
    ScoreableTransaction.empty.copy(customerId = _customerId, analysisAmount = _analysisAmount, analysisDate = java.sql.Date.valueOf(_transactionDate))

  import ExcelUtils._
  import com.quantexa.example.scoring.batch.utils.fiu.CustomerDateFactsTestData._

  val rawDataWorkbookSheet = workbook.getSheet("Raw Data")
  val rows = rawDataWorkbookSheet.iterator().asScala.toSeq.tail

  val customerATransactions = rows.map {
    row =>
      toScoreableTransaction(row.getCell(0).asString,
        row.getCell(1).asDouble,
        row.getCell(2).asDateString)
  }

  val customerBTransactions = customerATransactions.map(_.copy(customerId = "B"))
  val customers_A_B_Transactions = scala.util.Random.shuffle(customerATransactions ++ customerBTransactions)
}

object PreviousCountriesTestData {
  implicit def localDateToSqlDate(s: String): java.sql.Date = new java.sql.Date(
    LocalDate.parse(s).toDateTimeAtStartOfDay(jodaTzUTC).getMillis)

  val jodaTzUTC = DateTimeZone.forID("UTC")

  def toScoreableTransaction(_customerId: String,
                             _analysisAmount: Double,
                             _transactionDate: String,
                             maybeCustomerOriginatorOrBeneficiary: Option[String],
                             maybeOriginatorCountry: Option[String],
                             maybeBeneficiaryCountry: Option[String]) = maybeCustomerOriginatorOrBeneficiary match {
    case Some("originator") => ScoreableTransaction.empty.copy(
      customerId = _customerId,
      analysisAmount = _analysisAmount,
      customerOriginatorOrBeneficiary = maybeCustomerOriginatorOrBeneficiary,
      analysisDate = java.sql.Date.valueOf(_transactionDate),
      customerAccountCountry = maybeOriginatorCountry,
      counterPartyAccountCountry = maybeBeneficiaryCountry)
    case Some("beneficiary") => ScoreableTransaction.empty.copy(
      customerId = _customerId,
      analysisAmount = _analysisAmount,
      customerOriginatorOrBeneficiary = maybeCustomerOriginatorOrBeneficiary,
      analysisDate = java.sql.Date.valueOf(_transactionDate),
      customerAccountCountry = maybeBeneficiaryCountry,
      counterPartyAccountCountry = maybeOriginatorCountry)

  }

  import com.quantexa.example.scoring.batch.utils.fiu.CustomerDateFactsTestData._
  import ExcelUtils._

  val rawDataWorkbookSheet = workbook.getSheet("Raw Data Countries")
  val rows = rawDataWorkbookSheet.iterator().asScala.toList.drop(10)

  val customerATransactions = rows.map {
    row =>
      toScoreableTransaction(row.getCell(0).asString,
        row.getCell(1).asDouble,
        row.getCell(2).asDateString,
        row.getCell(5).asOptionString,
        row.getCell(3).asOptionString,
        row.getCell(4).asOptionString)
  }
}
