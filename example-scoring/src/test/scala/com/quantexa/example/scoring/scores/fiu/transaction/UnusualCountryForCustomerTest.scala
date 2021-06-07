package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData.testTransactionFacts
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class UnusualCountryForCustomerTest extends FlatSpec with Matchers {

  val MaxSeverity = 100
  implicit val scoreInput: ScoreInput = ScoreInput.empty

  "UnusualCountryForCustomerTest" should "have UnusualBeneficiaryCountryForCustomer score transactions which include a country that has not been used before by the customer" in {
    val actualScoreResult = UnusualBeneficiaryCountryForCustomer.score(transaction = testTransactionFacts.copy(
        customerOriginatorOrBeneficiary = "originator", counterPartyAccountCountry = Some("DE"))
      )
    actualScoreResult.flatMap(_.severity).getOrElse(-1) shouldBe MaxSeverity
  }

  "UnusualCountryForCustomerTest" should "have UnusualOriginatingCountryForCustomer score transactions which include a country that has not been used before by the customer" in {
    val actualScoreResult = UnusualOriginatingCountryForCustomer.score(
        transaction = testTransactionFacts.copy(
          customerOriginatorOrBeneficiary = "beneficiary", counterPartyAccountCountry = Some("DE")
        )
      )

    actualScoreResult.flatMap(_.severity).getOrElse(-1) shouldBe MaxSeverity
  }

  "UnusualCountryForCustomerTest" should "have UnusualCounterpartyCountryForCustomer score transactions which include a country that has not been used before by the customer" in {
    val actualScoreResult = UnusualCounterpartyCountryForCustomer.score(
        transaction = testTransactionFacts.copy(
          counterPartyAccountCountry = Some("DE")
        )
      )

    actualScoreResult.flatMap(_.severity).getOrElse(-1) shouldBe MaxSeverity
  }

  ignore should "have UnusualBeneficiaryCountryForCustomer not score transactions which include a country already used by the customer" in {
    val actualScoreResult = UnusualBeneficiaryCountryForCustomer.score(
        transaction = testTransactionFacts.copy(
          customerOriginatorOrBeneficiary = "originator", counterPartyAccountCountry = Some("US")
        )
      )

    actualScoreResult shouldBe None
  }

  ignore should "have UnusualOriginatingCountryForCustomer not score transactions which include a country already used by the customer" in {
    val actualScoreResult = UnusualOriginatingCountryForCustomer.score(
        transaction = testTransactionFacts.copy(
          customerOriginatorOrBeneficiary = "beneficiary", counterPartyAccountCountry = Some("UK")
        )
      )

    actualScoreResult shouldBe None
  }

  ignore should "have UnusualCounterpartyCountryForCustomer not score transactions which include a country already used by the customer" in {
    val actualScoreResult = UnusualCounterpartyCountryForCustomer.score(
        transaction = testTransactionFacts.copy(
          counterPartyAccountCountry = Some("UK")
        )
      )

    actualScoreResult shouldBe None
  }

}