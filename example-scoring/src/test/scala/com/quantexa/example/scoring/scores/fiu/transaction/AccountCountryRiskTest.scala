package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData.testTransactionFacts
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{IntegerScoreParameter, ParameterIdentifier}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class AccountCountryRiskTest extends FlatSpec with Matchers {

  implicit val scoreInput: ScoreInput = ScoreInput.empty.copy(parameters = Map(
    ParameterIdentifier(None, "AccountCountryRisk_MediumRiskSeverity") -> IntegerScoreParameter("AccountCountryRisk_MediumRiskSeverity", "test", 50),
    ParameterIdentifier(None, "MediumRiskThresholdCorruptionScore") -> IntegerScoreParameter("MediumRiskThresholdCorruptionScore", "test", 77),
    ParameterIdentifier(None, "HighRiskThresholdCorruptionScore") -> IntegerScoreParameter("HighRiskThresholdCorruptionScore", "test", 44)
  ))

  "CustomerAccountCountryRisk" should "score high risk customer countries" in {
    val actualScoreResult = CustomerAccountCountryRisk.score(
      transaction = testTransactionFacts.copy(customerAccountCountryCorruptionIndex = Some(12))
    )
    assert(actualScoreResult.flatMap(_.severity).contains(100))
  }

  "CustomerAccountCountryRisk" should "score high risk counterparty countries" in {
    val actualScoreResult = CounterpartyAccountCountryRisk.score(
      transaction = testTransactionFacts.copy(counterPartyAccountCountryCorruptionIndex = Some(12))
    )
    assert(actualScoreResult.flatMap(_.severity).contains(100))
  }

  "CustomerAccountCountryRisk" should "not score low risk customer countries" in {
    val actualScoreResult = CustomerAccountCountryRisk.score(
      transaction = testTransactionFacts.copy(customerAccountCountryCorruptionIndex = Some(92))
    )
    assert(actualScoreResult.isEmpty)
  }

  "CustomerAccountCountryRisk" should "not score low risk counterparty countries" in {
    val actualScoreResult = CounterpartyAccountCountryRisk.score(
      transaction = testTransactionFacts.copy(counterPartyAccountCountryCorruptionIndex = Some(92))
    )
    assert(actualScoreResult.isEmpty)
  }

}