package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData.testTransactionFacts
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TransactionIsRoundAmountTest extends FlatSpec with Matchers {

  implicit val scoreInput: ScoreInput = ScoreInput.empty

  "TransactionIsRoundAmount" should "score the transaction if the transaction amount in original currency is a multiple of 1,000" in {
    val actualScoreResult = TransactionIsRoundAmount.score(
      transaction = testTransactionFacts.copy(amountOrigCurrency = 2000)
    )
    actualScoreResult.flatMap(_.severity).isDefined shouldBe true
  }

  "TransactionIsRoundAmount" should "not create score output if the transaction amount in original currency is not a multiple of 1,000" in {
    val actualScoreResult = TransactionIsRoundAmount.score(
      transaction = testTransactionFacts.copy(amountOrigCurrency = 2020.99)
    )
    actualScoreResult shouldBe None

  }
}
