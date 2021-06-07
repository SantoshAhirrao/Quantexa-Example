package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.utils.Counterparty
import com.quantexa.example.scoring.utils.ScoringUtils.getCountry
import com.quantexa.scoring.framework.model.ScoreModel._
import com.quantexa.etl.address.core.Lexicons._
import com.quantexa.example.scoring.model.fiu.FactsModel.{CustomerDateFacts, TransactionFacts}
import com.quantexa.example.scoring.model.fiu.RollupModel.TransactionScoreOutput

trait UnusualCountryForCustomer extends AugmentedDocumentScore[TransactionFacts, TransactionScoreOutput] {

  def id: String

  def name: String

  def scoreDescription(country: String, customerName: String, customerId: String): String = {
    if (customerName == null) s"Customer ($customerId) has not used $country as a $countryType country before."
    else
      s"Customer $customerName ($customerId) has not used $country as a $countryType country before."
  }

  def createExtraDescriptionText(countryCode: String): Map[String, String] = {
    Map("country" -> CountryISO3ToName.getOrElse(countryCode.toUpperCase, countryCode))
  }

  def score(transaction: TransactionFacts)(implicit scoreInput: ScoreInput): Option[TransactionScoreOutput] = {
    val country = getAccountCountryOfInterest(transaction)

    val prevUsedCountries = transaction.customerDateFacts.map(getPreviouslyPaidCountries)

    country.flatMap { ctry =>
      if (prevUsedCountries.exists(_.filterKeys(_ == ctry).isEmpty)) {
        Some(TransactionScoreOutput(keys = transaction.transactionKeys,
          severity = Some(100),
          band = None,
          extraDescriptionText = createExtraDescriptionText(ctry),
          description = Some(scoreDescription(ctry, transaction.customerName, transaction.customerId))))
      } else None
    }
  }

  def getAccountCountryOfInterest(transaction: TransactionFacts): Option[String]

  def countryType: String

  def getPreviouslyPaidCountries(customerDateFact: CustomerDateFacts): collection.Map[String, Long]
}

object UnusualBeneficiaryCountryForCustomer extends UnusualCountryForCustomer {
  def id = "UnusualBeneficiaryCountryForCustomer"

  def name = "Unusual beneficiary country for customer"

  def countryType = "beneficiary"

  def getAccountCountryOfInterest(transaction: TransactionFacts): Option[String] = {
    if (transaction.customerOriginatorOrBeneficiary == "originator") getCountry(Counterparty, transaction)
    else None
  }

  def getPreviouslyPaidCountries(customerDateFact: CustomerDateFacts): collection.Map[String, Long] = customerDateFact.previousBeneficiaryCountries.holder
}

object UnusualOriginatingCountryForCustomer extends UnusualCountryForCustomer {
  def id = "T103_UnusualOriginatingCountryForCustomer"

  def name = "Unusual originator country for customer"

  def countryType = "originator"

  def getAccountCountryOfInterest(transaction: TransactionFacts): Option[String] = {
    if (transaction.customerOriginatorOrBeneficiary == "beneficiary") getCountry(Counterparty, transaction) else None
  }

  def getPreviouslyPaidCountries(customerDateFact: CustomerDateFacts): collection.Map[String, Long] = customerDateFact.previousOriginatorCountries.holder
}

object UnusualCounterpartyCountryForCustomer extends UnusualCountryForCustomer {
  def id = "T102_UnusualCounterpartyCountryForCustomer"

  def name = "Unusual counter party country for customer"

  def countryType = "counterparty"

  def getAccountCountryOfInterest(transaction: TransactionFacts) = getCountry(Counterparty, transaction)

  def getPreviouslyPaidCountries(customerDateFact: CustomerDateFacts): collection.Map[String, Long] = {
    customerDateFact.previousOriginatorCountries.holder ++ customerDateFact.previousBeneficiaryCountries.holder
  }
}
