package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData.testTransactionFacts
import com.quantexa.scoring.framework.model.ScoreModel.{BasicScoreOutput, ScoreInput}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class RoundAmountTransactionOfHighRiskCustomerTest extends FlatSpec with Matchers {
  behavior of RoundAmountTransactionOfHighRiskCustomer.id
  it should "score trigger if the transaction is a round amount and of a high risk customer" in {
    val actualScoreResult = RoundAmountTransactionOfHighRiskCustomer.score(transaction = testTransactionFacts)(
      ScoreInput.empty.copy(
        previousScoreOutput = Map(
          TransactionIsRoundAmount -> BasicScoreOutput(Some(100), None, None),
          TransactionOfHighRiskCustomer -> BasicScoreOutput(Some(20), None, None))
      ))
    actualScoreResult.isDefined shouldBe true
  }

  it should "not trigger if the transaction is a round amount but not of a high risk customer" in {
    val actualScoreResult = RoundAmountTransactionOfHighRiskCustomer.score(transaction = testTransactionFacts)(
      ScoreInput.empty.copy(
        previousScoreOutput = Map(
          TransactionIsRoundAmount -> BasicScoreOutput(Some(100), None, None))
      ))
    actualScoreResult.isDefined shouldBe false
  }

  it should "not trigger if the transaction is not a round amount but is of a high risk customer" in {
    val actualScoreResult = RoundAmountTransactionOfHighRiskCustomer.score(transaction = testTransactionFacts)(
      ScoreInput.empty.copy(
        previousScoreOutput = Map(
          TransactionOfHighRiskCustomer -> BasicScoreOutput(Some(100), None, None))
      ))
    actualScoreResult.isDefined shouldBe false
  }

  it should "not trigger if the transaction is not a round amount and the is not of a high risk customer" in {
    val actualScoreResult = RoundAmountTransactionOfHighRiskCustomer.score(transaction = testTransactionFacts)(
      ScoreInput.empty)
    actualScoreResult.isDefined shouldBe false
  }
}
