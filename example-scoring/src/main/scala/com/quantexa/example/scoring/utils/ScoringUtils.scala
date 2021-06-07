package com.quantexa.example.scoring.utils

import com.quantexa.example.scoring.model.fiu.FactsModel.TransactionFacts

object ScoringUtils {
  def getCountry(custOrCtpy: CustomerOrCounterparty, transaction: TransactionFacts):Option[String] = custOrCtpy match {
    case Customer => transaction.customerAccountCountry
    case Counterparty => transaction.counterPartyAccountCountry
  }

  /**
    *
    * @param tuple1 Tuple (String, Int)
    * @param tuple2 Tuple (String, Int)
    * @return True when the value of the first tuple is greater than the value of the second tuple or, if they are equal,
    *         when the key of the first tuple is less than the key of the second tuple. False otherwise
    */
  def sortByValueAndByKey(tuple1: (String, Int), tuple2: (String, Int)): Boolean = {
    if(tuple1._2 != tuple2._2) tuple1._2 > tuple2._2
    else tuple1._1 < tuple2._1
  }

  /**
    * @param date                 - date to check if it is (or is not) in the lookback period
    * @param lookbackFromThisDate - the date to look back from
    * @param monthsToLookBack     - the number of months to look back from the lookbackFromThisDate date
    * @return True if the date is in the lookback period, false otherwise
    */
  def checkIfDateInLookbackPeriod(date: java.sql.Date, lookbackFromThisDate: java.sql.Date, monthsToLookBack: Int): Boolean =
    date.getTime >= scoringLookbackPeriodStartDateTime(lookbackFromThisDate.getTime, monthsToLookBack) && date.getTime <= lookbackFromThisDate.getTime

  private def scoringLookbackPeriodStartDateTime(scoringRunDateTimeInMillis: Long, monthsToLookback: Int): Long =
    scoringRunDateTimeInMillis - lookbackPeriodInMillis(monthsToLookback)

  private def lookbackPeriodInMillis(months: Int): Long = months * 30 * 24 * 60 * 60 * 1000L
}

sealed trait CustomerOrCounterparty

private[scoring] case object Customer extends CustomerOrCounterparty

private[scoring] case object Counterparty extends CustomerOrCounterparty


sealed trait OriginatorOrBeneficiary

private[scoring] case object Originator extends OriginatorOrBeneficiary

private[scoring] case object Beneficiary extends OriginatorOrBeneficiary