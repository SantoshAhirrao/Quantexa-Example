package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.example.scoring.model.fiu.FactsModel.CategoryCounter
import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData.testTransactionFacts
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.scalatest.{FlatSpec, Matchers}

class UnusualCounterpartyCountryForCustomerTest extends FlatSpec with Matchers {

  private val MaxSeverity = 100
  private val triggered = Some(MaxSeverity)
  private val notTriggered = None
  private val newOriginator = "DE"
  private val nonNewOriginator = "UK"
  private val newBeneficiary = "RUS"
  private val nonNewBeneficiary = "US"

  implicit val scoreInput: ScoreInput = ScoreInput.empty

  "UnusualCounterpartyCountryForCustomerTest" should "trigger when a transactions has a new beneficiary and the customer is the originator" in {
    val actualScoreResult = UnusualCounterpartyCountryForCustomer.score(transaction = testTransactionFacts.copy(
      customerOriginatorOrBeneficiary = "originator", counterPartyAccountCountry = Some(newBeneficiary))

    )
    actualScoreResult.flatMap(_.severity) shouldBe triggered
  }

  it should "trigger when a transaction has a new originator and the customer is the beneficiary" in {
    val actualScoreResult = UnusualCounterpartyCountryForCustomer.score(
      transaction = testTransactionFacts.copy(
        customerOriginatorOrBeneficiary = "beneficiary", counterPartyAccountCountry = Some(newOriginator))
    )
    actualScoreResult.flatMap(_.severity) shouldBe triggered
  }

  it should "not trigger when the transaction doesn't have a new originator and the customer is the beneficiary" in {
    val actualScoreResult = UnusualCounterpartyCountryForCustomer.score(
      transaction = testTransactionFacts.copy(
        customerOriginatorOrBeneficiary = "beneficiary",
        counterPartyAccountCountry = Some(nonNewOriginator),
        customerDateFacts = testTransactionFacts.customerDateFacts.map(_.copy(previousOriginatorCountries = CategoryCounter(Map(nonNewOriginator -> 7))))
      ) //Map containing "ISO3 country code -> number of times the customer has received transactions from this country from the beginning of time up to but NOT including today"
    )
    actualScoreResult.flatMap(_.severity) shouldBe notTriggered
  }

  it should "not trigger when the transaction doesn't have a new beneficiary and the customer is the originator" in {
    val actualScoreResult = UnusualCounterpartyCountryForCustomer.score(
      transaction = testTransactionFacts.copy(
        customerOriginatorOrBeneficiary = "originator",
        counterPartyAccountCountry = Some(nonNewBeneficiary),
        customerDateFacts = testTransactionFacts.customerDateFacts.map(_.copy(previousBeneficiaryCountries = CategoryCounter(Map(nonNewBeneficiary -> 7))))
      ))
    actualScoreResult.flatMap(_.severity) shouldBe notTriggered
  }
}
