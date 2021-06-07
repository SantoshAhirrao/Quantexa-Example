package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData.testTransactionFacts
import com.quantexa.scoring.framework.model.ScoreModel.{BasicScoreOutput, ScoreInput}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TransactionLowToHighRiskCountryTest extends FlatSpec with Matchers {

  val MaxSeverity = 100

  "TxnFromLowToHighRiskCountry" should "not score transactions from high risk countries to low risk countries" in {
    val actualScoreOutput = TxnFromLowToHighRiskCountry.score(
      transaction = testTransactionFacts.copy(customerOriginatorOrBeneficiary = "originator")
    )(
      ScoreInput.empty.copy(
        previousScoreOutput = Map(
          CustomerAccountCountryRisk -> BasicScoreOutput(Some(100), Some("high"), Some("test"))
        )
      )
    )

    actualScoreOutput shouldBe None
  }

  "TxnFromLowToHighRiskCountry" should "score transactions from low risk countries to high risk countries" in {
    val actualScoreOutput = TxnFromLowToHighRiskCountry.score(
      transaction = testTransactionFacts.copy(
        customerOriginatorOrBeneficiary = "originator")
    )(
      ScoreInput.empty.copy(
        previousScoreOutput = Map(
          CounterpartyAccountCountryRisk -> BasicScoreOutput(Some(100), Some("high"), Some("test"))
        )
      )
    )
    actualScoreOutput.flatMap(_.severity).getOrElse(-1) shouldBe MaxSeverity
  }

  "TxnFromLowToHighRiskCountry" should "not score transactions from high risk countries to high risk countries" in {
    val actualScoreOutput = TxnFromLowToHighRiskCountry.score(
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
    actualScoreOutput.flatMap(_.severity).getOrElse(-1) shouldBe -1
  }

  "TxnFromLowToHighRiskCountry" should "not score transactions from low risk countries to low risk countries" in {
    val actualScoreOutput = TxnFromLowToHighRiskCountry.score(
      transaction = testTransactionFacts.copy(
        customerOriginatorOrBeneficiary = "originator"))(ScoreInput.empty)
    actualScoreOutput.flatMap(_.severity).getOrElse(-1) shouldBe -1
  }

}