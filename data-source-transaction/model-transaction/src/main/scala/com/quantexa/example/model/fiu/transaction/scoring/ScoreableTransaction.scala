package com.quantexa.example.model.fiu.transaction.scoring

import com.quantexa.example.model.fiu.transaction.TransactionModel.TransactionAccount

trait TxnFactFields {
  def transactionId: String
  def customerId: String
  //TODO: Add in customerName to required fields and fix scores
  //def customerName: Option[String]
  def analysisDate: java.sql.Date
  def analysisAmount: Double
  def amountOrigCurrency: Double
  def origCurrency: String
  def customerAccountCountry: Option[String]
  def customerAccountCountryCorruptionIndex: Option[Int]
  def counterPartyAccountCountry: Option[String]
  def counterPartyAccountCountryCorruptionIndex: Option[Int]
}
case class ScoreableTransaction(transactionId: String,
                                customerId: String,
                                customerName: Option[String],
                                debitCredit: String,
                                customerOriginatorOrBeneficiary: Option[String],
                                postingDate: java.sql.Date,
                                analysisDate: java.sql.Date,
                                monthsSinceEpoch: Long, // dateUnitColumn for CustomerMonthFacts
                                amountOrigCurrency: Double,
                                origCurrency: String,
                                amountInGBP: Double,
                                analysisAmount: Double,
                                customerAccount : TransactionAccount,
                                counterpartyAccount: Option[TransactionAccount],
                                customerAccountCountry: Option[String], //TODO: Remove and use from customerAccount
                                customerAccountCountryCorruptionIndex: Option[Int],
                                counterPartyAccountCountry: Option[String], //TODO: Remove and use from customerAccount
                                counterPartyAccountCountryCorruptionIndex: Option[Int],
                                description: Option[String],
                                counterpartyAccountId: String,
                                counterpartyCustomerId: Option[String],
                                paymentToCustomersOwnAccountAtBank: Option[Boolean]) extends TxnFactFields{
}

object ScoreableTransaction {
  def empty = ScoreableTransaction(transactionId = "",
    customerId = "",
    customerName = None,
    debitCredit = "",
    customerOriginatorOrBeneficiary = None,
    postingDate = java.sql.Date.valueOf("1960-01-01"),
    analysisDate = java.sql.Date.valueOf("1960-01-01"),
    monthsSinceEpoch = 0,
    amountOrigCurrency = 0,
    origCurrency = "",
    amountInGBP = 0,
    analysisAmount = 0,
    customerAccount = TransactionAccount("","",None,None,None,None,None,None,None),
    customerAccountCountry = None,
    customerAccountCountryCorruptionIndex = None,
    counterpartyAccount = None,
    counterPartyAccountCountry = None,
    counterPartyAccountCountryCorruptionIndex = None,
    description = None,
    counterpartyAccountId = "",
    counterpartyCustomerId = None,
    paymentToCustomersOwnAccountAtBank = None)
}
