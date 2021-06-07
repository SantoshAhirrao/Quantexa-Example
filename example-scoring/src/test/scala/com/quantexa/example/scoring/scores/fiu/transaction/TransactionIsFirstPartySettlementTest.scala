package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData.testTransactionFacts
import com.quantexa.example.scoring.utils.TypedConfigReader
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.scalatest.{FlatSpec, Matchers}

class TransactionIsFirstPartySettlementTest extends FlatSpec with Matchers {

  val config = TypedConfigReader.getValuesFromConfig()
  System.setProperty("scoreOutputRoot", config.hdfsFolderScoring)

  private val MaxSeverity = 100
  private val triggered = Some(MaxSeverity)
  private val notTriggered = None

  implicit val scoreInput: ScoreInput = ScoreInput.empty

  "TransactionIsFirstPartySettlementTest" should "trigger when the customer_id on the originator and counterparty account are the same." in {
    val actualScoreResult = TransactionIsFirstPartySettlement.score(transaction = testTransactionFacts.copy(
      paymentToCustomersOwnAccountAtBank = Some(true)
    ))
    actualScoreResult.flatMap(_.severity) shouldBe triggered
  }


  it should "not trigger when which has a new originator is not triggered customer ids are different." in {
    val actualScoreResult = TransactionIsFirstPartySettlement.score(
      transaction = testTransactionFacts.copy(
        paymentToCustomersOwnAccountAtBank = Some(false)
      ))
    actualScoreResult.flatMap(_.severity) shouldBe notTriggered
  }
}
