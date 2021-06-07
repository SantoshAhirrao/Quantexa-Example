package com.quantexa.example.scoring.model.fiu

import com.quantexa.analytics.scala.scoring.model.ScoringModel.{KeyedBasicScoreOutput, KeyedScoreOutput, LookupKey}
import com.quantexa.example.model.fiu.customer.CustomerExtractor
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.model.fiu.hotlist.HotlistExtractor
import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist
import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.model.fiu.scoring.DocumentAttributes.{CustomerAttributes, HotlistAttributes}
import com.quantexa.example.model.fiu.scoring.EntityAttributes._
import com.quantexa.scoring.framework.model.LookupModel.{KeyAnnotation, LookupSchema}
import com.quantexa.scoring.framework.model.ScoreDefinition.Model._


object ScoringModel {
  val coreDocumentTypes: DocumentTypes = Seq(
    DocumentTypeInformation(documentSource[Customer], documentAttributes[CustomerAttributes], CustomerExtractor),
    DocumentTypeInformation(documentSource[Hotlist], documentAttributes[HotlistAttributes], HotlistExtractor))

  val batchOnlyDocumentTypes: DocumentTypes = Seq(DocumentTypeInformation(documentSource[Customer], documentAttributes[CustomerAttributes], CustomerExtractor))

  val coreEntityTypes = Seq(
    entityAttributes[Individual], entityAttributes[Business], entityAttributes[Telephone], entityAttributes[Account], entityAttributes[Address])

  //Keys in Data
  trait CustKey {
    def customerId: String

    def CustomerId = "customerId"

    def CustomerDocType = "customer"
  }

  trait AnalysisDateKey {
    def analysisDate: java.sql.Date

    def AnalysisDate = "analysisDate"

  }

  trait CalendarMonthEndDateKey {
    def calendarMonthEndDate: java.sql.Date

    def CalendarMonthEndDate = "calendarMonthEndDate"
  }

  trait TransactionKey {
    def transactionId: String

    def TransactionId = "transactionId"

    def TransactionDocType = "transaction"
  }

  trait AggregatedTransactionKey {
    def aggregatedTransactionId: String
  }


  case class CustomerKey(customerId: String) extends CustKey

  //TODO: IP-516 Remove analysisDate and use transactionDate.
  case class CustomerDateKey(customerId: String, analysisDate: java.sql.Date) extends CustKey with AnalysisDateKey
  object CustomerDateKey {
    def createStringKey(customerId: String, analysisDate: java.sql.Date): String = {
      s"$customerId|$analysisDate"
    }
  }

  case class CustomerMonthKey(customerId: String, calendarMonthEndDate: java.sql.Date) extends CustKey with CalendarMonthEndDateKey
  object CustomerMonthKey {
    def createStringKey(customerId: String, calendarMonthEndDate: java.sql.Date): String = {
      s"$customerId|$calendarMonthEndDate"
    }
  }

  case class TransactionKeys(transactionId: String, customerId: String, analysisDate: java.sql.Date) extends CustKey with AnalysisDateKey with TransactionKey

  case class AggregatedTransactionScoreOutputKeys(aggregatedTransactionId: String, customerId: String, counterpartyAccountId: String) extends CustKey with AggregatedTransactionKey

  //Case Classes that are used as output from scores that run off fact tables
  case class CustomerScoreOutputKeys(customerId: String) extends CustKey

  case class CustomerScoreOutput(keys: CustomerScoreOutputKeys,
                                 severity: Option[Int] = None,
                                 band: Option[String] = None,
                                 description: Option[String] = None) extends KeyedScoreOutput[CustomerScoreOutputKeys] {
    def toKeyedBasicScoreOutput: KeyedBasicScoreOutput = KeyedBasicScoreOutput(
      keyValues = Seq(LookupKey(name = this.keys.CustomerId, stringKey = Some(this.keys.customerId))),
      docType = this.keys.CustomerDocType,
      severity = this.severity,
      band = this.band,
      description = this.description)
  }

  case class AggregatedTransactionScoreOutput(keys: AggregatedTransactionScoreOutputKeys,
                                              severity: Option[Int] = None,
                                              band: Option[String] = None,
                                              description: Option[String] = None) extends KeyedScoreOutput[AggregatedTransactionScoreOutputKeys] {
    def toKeyedBasicScoreOutput = KeyedBasicScoreOutput(
      keyValues = Seq(
        LookupKey(name = "aggregatedTransactionId", stringKey = Some(this.keys.aggregatedTransactionId))),
      docType = "aggregatedtransaction",
      severity = this.severity,
      band = this.band,
      description = this.description)
  }

  case class AggregatedTransactionScoreOutputWithUnderlying(keys: AggregatedTransactionScoreOutputKeys,
                                                            severity: Option[Int],
                                                            band: Option[String],
                                                            description: Option[String],
                                                            underlyingScores: Seq[KeyedBasicScoreOutput]) extends KeyedScoreOutput[AggregatedTransactionScoreOutputKeys] {
    def toKeyedBasicScoreOutput = KeyedBasicScoreOutput(
      keyValues = Seq(
        LookupKey(name = "aggregatedTransactionId", stringKey = Some(this.keys.aggregatedTransactionId))),
      docType = "aggregatedtransaction",
      severity = this.severity,
      band = this.band,
      description = this.description)
  }

  //Other Case Classes

  case class AggregatedCustomerTransactionLookup(
                                                  @KeyAnnotation cus_id_no: Long, cus_id_no_string: String, no_txns_3m: Option[Int], sum_txns_value_3m: Option[Double], max_txn_value_3m: Option[Double]) extends LookupSchema

  //TODO: IP-517 Move to common models
  case class CountryCorruption(ISO3: String,
                               corruptionIndex: Int)

  case class DistinctCountryScoreOutput(severity: Option[Int] = None,
                                        band: Option[String] = None,
                                        description: Option[String] = None,
                                        countries: Seq[String] = Seq.empty)

}
