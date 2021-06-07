package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData.testTransactionFacts
import com.quantexa.scoring.framework.model.ScoreModel.{BasicScoreOutput, ScoreInput}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TransactionHighToLowRiskCountryTest extends FlatSpec with Matchers {

  val MaxSeverity = 100

  "TransactionFromHighToLowRiskCountry" should "score transactions from high risk countries to low risk countries" in {
    val actualScoreResult = TxnFromHighToLowRiskCountry.score(
      transaction = testTransactionFacts.copy(customerOriginatorOrBeneficiary = "originator")
    )(
      ScoreInput.empty.copy(
        previousScoreOutput = Map(
          CustomerAccountCountryRisk -> BasicScoreOutput(Some(100), Some("high"), Some("test"))
        )
      )
    )
    actualScoreResult.flatMap(_.severity).getOrElse(-1) shouldBe MaxSeverity
  }


  "TransactionFromHighToLowRiskCountry" should "not score transactions from low risk countries to high risk countries" in {
    val actualScoreResult = TxnFromHighToLowRiskCountry.score(transaction = testTransactionFacts.copy(customerOriginatorOrBeneficiary = "originator"))(ScoreInput.empty.copy(
      previousScoreOutput = Map(
        CounterpartyAccountCountryRisk -> BasicScoreOutput(Some(100), Some("high"), Some("test")))))
    actualScoreResult shouldBe None
  }

  "TransactionFromHighToLowRiskCountry" should "not score transactions from high risk countries to high risk countries" in {
    val actualScoreResult = TxnFromHighToLowRiskCountry.score(
      transaction = testTransactionFacts.copy(
        customerOriginatorOrBeneficiary = "originator")
    )(
      ScoreInput.empty.copy(
        previousScoreOutput = Map(
          CounterpartyAccountCountryRisk -> BasicScoreOutput(Some(100), Some("high"), Some("test")),
          CustomerAccountCountryRisk -> BasicScoreOutput(Some(100), Some("high"), Some("test"))
        )
      )
    )
    actualScoreResult.flatMap(_.severity).getOrElse(-1) shouldBe -1
  }

  "TransactionFromHighToLowRiskCountry" should "not score transactions from low risk countries to low risk countries" in {
    val actualScoreResult = TxnFromHighToLowRiskCountry.score(
      transaction = testTransactionFacts.copy(
        customerOriginatorOrBeneficiary = "originator"))(ScoreInput.empty)
    actualScoreResult.flatMap(_.severity).getOrElse(-1) shouldBe -1
  }


}