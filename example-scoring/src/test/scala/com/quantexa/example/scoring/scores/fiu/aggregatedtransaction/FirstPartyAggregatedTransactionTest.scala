package com.quantexa.example.scoring.scores.fiu.aggregatedtransaction

import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData._
import org.scalatest.{FlatSpec, Matchers}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput

class FirstPartyAggregatedTransactionTest extends FlatSpec with Matchers {

  implicit val scoreInput: ScoreInput = ScoreInput.empty

  "FirstPartyAggregatedTransactionTest" should "trigger when paymentToCustomersOwnAccountAtBank contains true" in {
    val actualScoreResult = FirstPartyAggregatedTransaction.score(aggregatedTransaction = testAggregationTransactionFacts.copy(
      paymentToCustomersOwnAccountAtBank = Seq(true)
    ))
    actualScoreResult.flatMap(_.severity) shouldBe Some(100)
  }

  it should "not trigger when paymentToCustomersOwnAccountAtBank does not contain true" in {
    val actualScoreResult = FirstPartyAggregatedTransaction.score(
      aggregatedTransaction = testAggregationTransactionFacts.copy(
        paymentToCustomersOwnAccountAtBank = Seq(false,false)
      ))
    actualScoreResult.flatMap(_.severity) shouldBe None
  }

}
