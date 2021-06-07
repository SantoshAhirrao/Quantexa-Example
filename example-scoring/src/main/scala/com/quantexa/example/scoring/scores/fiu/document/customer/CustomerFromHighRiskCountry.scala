package com.quantexa.example.scoring.scores.fiu.document.customer

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScoreWithLookup
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.model.fiu.lookupdatamodels.HighRiskCountryCode
import com.quantexa.example.scoring.model.fiu.ScoringModel.{CustomerScoreOutput, CustomerScoreOutputKeys}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.LookupParameters

object CustomerFromHighRiskCountry extends AugmentedDocumentScoreWithLookup[Customer, HighRiskCountryCode, CustomerScoreOutput] {

	def id = "CustomerFromHighRiskCountry"
  def name = "Customer From High Risk Country"
  
	def lookupParams: LookupParameters[Customer] = LookupParameters(x => x.residenceCountry.getOrElse(""), None)

  def createDescription(customerID: String, highRiskResidenceCountry: String): String = {
    s"Customer $customerID has residency in a high risk country ($highRiskResidenceCountry)."
  }

  def score(customer: Customer, lookup: HighRiskCountryCode)(implicit scoreInput: ScoreInput): Option[CustomerScoreOutput] = {
    val residenceCountry = customer.residenceCountry

    residenceCountry.flatMap {
      country =>
        if (lookup.riskCountryCode.contains(country)) {
          Some(CustomerScoreOutput(
            keys = CustomerScoreOutputKeys(customer.customerIdNumberString),
            description = Some(createDescription(customer.customerIdNumberString, residenceCountry.get)),
            severity = Some(100)))
        } else {
          None
        }
    }
  }
}
