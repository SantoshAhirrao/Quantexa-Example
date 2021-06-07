package com.quantexa.example.scoring.scores.fiu.document.customer

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScoreWithLookup
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.model.fiu.lookupdatamodels.PostcodePrice
import com.quantexa.example.scoring.model.fiu.ScoringModel.{CustomerScoreOutput, CustomerScoreOutputKeys}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.LookupParameters
import com.quantexa.scoring.framework.parameters.{ScoreParameterIdentifier, ParameterIdentifier, ScoreParameters}

/**
  * Score triggers if the average postal price of the postcode is lower than a given threshold; low value areas tend to be more risky for some
  * kinds of fraud
  **/
object CustomerAddressInLowValuePostcode extends AugmentedDocumentScoreWithLookup[Customer, PostcodePrice, CustomerScoreOutput] with ScoreParameters {

	def id = "CustomerAddressInLowValuePostcode"
	def name = "Customer Address in Low Value Postcode"

  def lookupParams: LookupParameters[Customer] = LookupParameters(x => x.parsedAddress.flatMap(x=>x.postcode).getOrElse(""), None)
  
  val parameters: Set[ScoreParameterIdentifier] = {
    Set(ParameterIdentifier(None,"CustomerAddressInLowValuePostcode_PriceThreshold"))
  }

  private def createDescription(customerID: String, postcode: String): String = {
    s"Customer $customerID is located in a high risk postcode ($postcode)."
  }

  def score(customer: Customer, lookup: PostcodePrice)(implicit scoreInput: ScoreInput): Option[CustomerScoreOutput] = {

    val priceThreshold = parameter[Double]("CustomerAddressInLowValuePostcode_PriceThreshold")

    val postcode = customer.parsedAddress.flatMap(_.postcode)
    postcode.flatMap {
      pcde =>
      if (lookup.price < priceThreshold) {
        Some(CustomerScoreOutput(
          keys = CustomerScoreOutputKeys(customer.customerIdNumberString),
          description = Some(createDescription(customer.customerIdNumberString, pcde)),
          severity = Some(100)))
      } else {
        None
      }
    }
  }

}
