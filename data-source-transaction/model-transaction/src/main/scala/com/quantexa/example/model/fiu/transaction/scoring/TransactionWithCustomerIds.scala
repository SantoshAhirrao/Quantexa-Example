package com.quantexa.example.model.fiu.transaction.scoring

import com.quantexa.example.model.fiu.transaction.TransactionModel.TransactionAccount
import com.quantexa.resolver.ingest.Model.id

case class TransactionWithCustomerIds(
                                      @id transactionId: String,
                                      customerId: String,
                                      counterpartyAccountId: String,
                                      counterpartyCustomerId: Option[String],
                                      originatingAccount: Option[TransactionAccount],
                                      beneficiaryAccount: Option[TransactionAccount],
                                      debitCredit: String,
                                      postingDate: java.sql.Timestamp,
                                      runDate: java.sql.Date,
                                      txnAmount: Double,
                                      txnCurrency: String,
                                      txnAmountInBaseCurrency: Double,
                                      baseCurrency: String,
                                      txnDescription: Option[String],
                                      sourceSystem: Option[String])
