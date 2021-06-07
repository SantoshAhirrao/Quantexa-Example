package com.quantexa.example.scoring.model.fiu

import com.quantexa.example.model.fiu.customer.scoring.ScoreableCustomer
import com.quantexa.example.model.fiu.transaction.TransactionModel.{AggregatedStats, AggregatedTransactionAccount}
import com.quantexa.example.model.fiu.transaction.scoring.TxnFactFields
import com.quantexa.example.scoring.model.fiu.ScoringModel._
import com.quantexa.resolver.ingest.Model.id
import com.quantexa.scoring.framework.model.LookupModel.{KeyAnnotation, LookupSchema}

object FactsModel {

//  case class TransactionKeys(transactionId: String, customerId: String, analysisDate: java.sql.Date) extends CustKey with AnalysisDateKey with TransactionKey

  case class TransactionFacts(@id transactionId: String,
                              customerId: String,
                              customerName: String,
                              analysisDate: java.sql.Date,
                              analysisAmount: Double,
                              amountOrigCurrency: Double,
                              origCurrency: String,
                              customerOriginatorOrBeneficiary: String,
                              customerAccountCountry: Option[String],
                              customerAccountCountryCorruptionIndex: Option[Int],
                              counterPartyAccountCountry: Option[String],
                              counterPartyAccountCountryCorruptionIndex: Option[Int],
                              customer: ScoreableCustomer,
                              customerDateFacts: Option[CustomerDateFacts],
                              paymentToCustomersOwnAccountAtBank: Option[Boolean]
                             ) extends TxnFactFields {
    def transactionKeys = TransactionKeys(transactionId = this.transactionId,
      analysisDate = this.analysisDate,
      customerId = this.customerId)
  }

  /**
    *
    * @param aggregatedTransactionId                  Primary Key for aggregatedTransaction, a concatenation of customerId and counterpartyAccountId
    * @param customerId                               Primary Key for a Customer
    * @param counterpartyAccountId                    Counterparty account with whom the customer is transacting
    * @param isCrossBorder                            True if originator account country equals beneficiary account country, false otherwise
    * @param currencies                               Distinct values of transactions' currencies
    * @param paymentToCustomersOwnAccountAtBank       Boolean per transaction in the aggregate indicating if the customer owns the counterparty account
    * @param numberOfTransactionsToDate               The total number of transactions up to and including today
    * @param numberOfTransactionsOverLast365Days      The total number of transactions over the past twelve months from the latest transaction of the full input dataset
    * @param totalTransactionsAmount                  The total amount of transactions up to and including today
    * @param minTransactionAmount                     The min transaction amount up to and including today
    * @param maxTransactionAmount                     The max transaction amount up to and including today
    * @param totalTransactionsAmountOverLast365Days   The total amount of transactions over the past twelve months from the latest transaction of the full input dataset
    * @param minTransactionAmountOverLast365Days      The min transaction amount over the past twelve months from the latest transaction of the full input dataset
    * @param maxTransactionAmountOverLast365Days      The max transaction amount over the past twelve months from the latest transaction of the full input dataset
    */
  case class AggregatedTransactionFacts(@id aggregatedTransactionId: String,
                                        customerId: String,
                                        counterpartyAccountId: String,
                                        isCrossBorder: Option[Boolean],
                                        currencies: Seq[String],
                                        paymentToCustomersOwnAccountAtBank: Seq[Boolean],
                                        numberOfTransactionsToDate: Long,
                                        numberOfTransactionsOverLast365Days: Long,
                                        totalTransactionsAmount: Double,
                                        minTransactionAmount: Option[Double],
                                        maxTransactionAmount: Option[Double],
                                        totalTransactionsAmountOverLast365Days: Double,
                                        minTransactionAmountOverLast365Days: Option[Double],
                                        maxTransactionAmountOverLast365Days: Option[Double]
                                       )

  case class CategoryCounter[T](holder: scala.collection.Map[T, Long])


  case class CustomerDateFacts(key: CustomerDateKey, //Today refers to the date within the key
                               @id @KeyAnnotation keyAsString: String, //Required for elastic
                               customerName: Option[String], //Name of listed customer
                               numberOfTransactionsToday: Long, //The total number of transactions today
                               numberOfTransactionsToDate: Long, //The total number of transactions up to and including today
                               numberOfPreviousTransactionsToDate: Long, //The total number of transactions up to but NOT including today
                               numberOfTransactionsOverLast7Days: Long, //The total number of transactions over the past 7 days (including today)
                               numberOfTransactionsOverLast30Days: Long, //The total number of transactions over the past 30 days (including today)
                               totalValueTransactionsToday: Double, //The total value of transactions today
                               totalTransactionValueToDate: Double, //The total value of transactions up to and including today
                               customerDebitTransactionEMAOverLast30Days: Option[Double],
                               customerDebitTransactionSMAOverLast30Days: Option[Double],
                               totalPreviousTransactionValueToDate: Double, //The total value of transactions up to but NOT including today
                               totalValueTransactionsOverLast7Days: Double, //The total value of transactions over the past 7 days (including today)
                               totalValueTransactionsOverLast30Days: Double, //The total value of transactions over the past 30 days (including today)
                               transactionValueStdDevToDate: Option[Double], //The population standard deviation of individual transaction values up to and including today
                               previousAverageTransactionVolumeToDate: Option[Double], //The average transaction value over a year up to but not including today
                               previousStdDevTransactionVolumeToDate: Option[Double], //The sample standard deviation of DAILY transaction volumes over a year up to but NOT including today
                               previousAverageTransactionVolumeLast7Days: Option[Double], //The average of 7 day total transaction volumes over a year up to but NOT including today
                               previousStdDevTransactionVolumeLast7Days: Option[Double], //The sample standard deviation of 7 day total transaction volumes over a year up to but NOT including today
                               previousAverageTransactionVolumeLast30Days: Option[Double], //The average of 30 day total transaction volumes over a year up to but NOT including today
                               previousStdDevTransactionVolumeLast30Days: Option[Double], //The sample standard deviation of 30 day total transaction volumes over a year up to but NOT including today
                               previousAverageTransactionAmountToDate: Option[Double], //The average of total DAILY transaction values over a year up to but NOT including today
                               previousStdDevTransactionAmountToDate: Option[Double], //The sample standard deviation of total DAILY transaction values over a year up to but NOT including today
                               previousAverageTransactionAmountLast7Days: Option[Double], //The average of 7 day total transaction values over a year up to but NOT including today
                               previousStdDevTransactionAmountLast7Days: Option[Double], //The sample standard deviation of 7 day total transaction values over a year up to but NOT including today
                               previousAverageTransactionAmountLast30Days: Option[Double], //The average of 30 day total transaction values over a year up to but NOT including today
                               previousStdDevTransactionAmountLast30Days: Option[Double], //The sample standard deviation of 30 day total transaction values over a year up to but NOT including today
                               originatorCountriesToday: CategoryCounter[String], //Map containing "ISO3 country code -> number of times the customer has received transactions from this country today"
                               beneficiaryCountriesToday: CategoryCounter[String], //Map containing "ISO3 country code -> number of times the customer has sent transactions to this country today"
                               previousOriginatorCountries: CategoryCounter[String] = CategoryCounter(Map.empty), //Map containing "ISO3 country code -> number of times the customer has received transactions from this country from the beginning of time up to but NOT including today"
                               previousBeneficiaryCountries: CategoryCounter[String] = CategoryCounter(Map.empty) //Map containing "ISO3 country code -> number of times the customer has sent transactions to this country from the beginning of time up to but NOT including today"
                              ) extends LookupSchema

  case class CustomerMonthFacts(key: CustomerMonthKey, //Today refers to the date within the key
                                @id @KeyAnnotation keyAsString: String, //Required for elastic
                                customerName: Option[String], //Name of listed customer
                                totalValueTransactionsThisCalendarMonth: Double, //The total value of transactions today
                                totalTransactionValueToDate: Double, //The total value of transactions up to and including today
                                averageMonthlyTxnValueOver6MonthsExclude1RecentMonth: Option[Double],  //Average value of transactions in the prior 6 months not including this month
                                minMonthlyTxnValueOver6MonthsExclude1RecentMonth: Option[Double], //Min value of transactions in the prior 6 months not including this month
                                maxMonthlyTxnValueOver6MonthsExclude1RecentMonth: Option[Double] //Max value of transactions in the prior 6 months not including this month
                               ) extends LookupSchema

  case class EMAFacts(key: CustomerDateKey, totalValueTransactionsToday: Double, customerDebitTransactionSMAOverLast30Days: Option[Double], customerDebitTransactionEMAOverLast30Days: Option[Double])

}

