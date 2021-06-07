package com.quantexa.example.scoring.scores.fiu.testdata

import com.quantexa.example.model.fiu.transaction.TransactionModel.AggregatedTransactionAccount
import com.quantexa.example.scoring.model.fiu.FactsModel._
import com.quantexa.example.scoring.model.fiu.ScoringModel.CustomerDateKey
import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.{aDate, testScoreableCustomer}

object FactsTestData {

  val testCustomerDateFacts = CustomerDateFacts(
    key = CustomerDateKey(customerId = "123", analysisDate = aDate.get),
    keyAsString = "yes this is a hack",
    customerName = Some("Jeff Andrews"),
    numberOfTransactionsToday = 1,
    numberOfTransactionsToDate = 2,
    numberOfPreviousTransactionsToDate = 1,
    numberOfTransactionsOverLast7Days = 3,
    numberOfTransactionsOverLast30Days = 5,
    totalValueTransactionsToday = 3,
    totalTransactionValueToDate = 5,
    customerDebitTransactionEMAOverLast30Days = None,
    customerDebitTransactionSMAOverLast30Days = None,    
    totalPreviousTransactionValueToDate = 10,
    totalValueTransactionsOverLast7Days = 5,
    totalValueTransactionsOverLast30Days = 5,
    transactionValueStdDevToDate = Some(3D),
    previousAverageTransactionVolumeToDate = Some(50.0),
    previousStdDevTransactionVolumeToDate = Some(50.0),
    previousAverageTransactionVolumeLast7Days = Some(50.0),
    previousStdDevTransactionVolumeLast7Days = Some(50.0),
    previousAverageTransactionVolumeLast30Days = Some(50.0),
    previousStdDevTransactionVolumeLast30Days = Some(50.0),
    previousAverageTransactionAmountToDate = Some(50.0),
    previousStdDevTransactionAmountToDate = Some(50.0),
    previousAverageTransactionAmountLast7Days = Some(50.0),
    previousStdDevTransactionAmountLast7Days = Some(50.0),
    previousAverageTransactionAmountLast30Days = Some(50.0),
    previousStdDevTransactionAmountLast30Days = Some(50.0),
    originatorCountriesToday = CategoryCounter(Map("UK" -> 1)),
    beneficiaryCountriesToday = CategoryCounter(Map("US" -> 2))
  )

  val testTransactionFacts = TransactionFacts(
    transactionId = "123",
    customerId = "133",
    customerName = "jeffrey",
    analysisDate = aDate.get,
    analysisAmount = 231.0,
    amountOrigCurrency = 280.3,
    origCurrency = "EUR",
    customerOriginatorOrBeneficiary = "D",
    customerAccountCountry = Some("ES"),
    customerAccountCountryCorruptionIndex = Some(70),
    counterPartyAccountCountry = Some("UK"),
    counterPartyAccountCountryCorruptionIndex = Some(30),
    customer = testScoreableCustomer,
    customerDateFacts = Some(testCustomerDateFacts),
    paymentToCustomersOwnAccountAtBank = None
  )

  val testAggregationTransactionFacts = AggregatedTransactionFacts(
    aggregatedTransactionId = "1|2",
    customerId = "1",
    counterpartyAccountId = "2",
    isCrossBorder = Some(false),
    currencies = Seq("GBP"),
    paymentToCustomersOwnAccountAtBank = Seq(true),
    numberOfTransactionsToDate = 2,
    numberOfTransactionsOverLast365Days = 2,
    totalTransactionsAmount = 5.0,
    minTransactionAmount = Some(5.0),
    maxTransactionAmount = Some(5.0),
    totalTransactionsAmountOverLast365Days = 5.0,
    minTransactionAmountOverLast365Days = Some(5.0),
    maxTransactionAmountOverLast365Days = Some(5.0)
  )
}
