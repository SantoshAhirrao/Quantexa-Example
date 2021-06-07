package com.quantexa.example.scoring.scores.fiu.rollup

import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerRollup, TransactionScoreOutput}
import com.quantexa.example.scoring.model.fiu.ScoringModel.{CustomerKey, TransactionKeys}
import com.quantexa.example.scoring.scores.fiu.transaction.TransactionIsRoundAmount
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{IntegerScoreParameter, ParameterIdentifier}
import org.scalatest.{FlatSpec, Matchers}

class CustomerRollupHighNumberOfRoundAmountBatchTest extends FlatSpec with Matchers {

  val scoreToTest = CustomerRollupHighNumberOfRoundAmountBatch(java.sql.Date.valueOf("2018-01-30"))
  implicit val outcomePeriod: ScoreInput = ScoreInput.empty.copy(parameters = Map(ParameterIdentifier(None,"outcomePeriodMonths") -> IntegerScoreParameter("outcomePeriodMonths","Outcome Period in months",1)))

  val baseTxnScoreTrigger = TransactionScoreOutput(keys=TransactionKeys("Txn001","Cust001",java.sql.Date.valueOf("2018-01-30")),severity=Some(100),band=None,description=None)

  "CustomerRollupHighNumberOfRoundAmount" should "take the maximum severity from 3 input scenario triggers in the outcome period" in {
    val threeScoresInOutcomePeriod:CustomerRollup[TransactionScoreOutput] = CustomerRollup(
      subject = "Cust001",
      keys = CustomerKey("Cust001"),
      customScoreOutputMap = Map(
        TransactionIsRoundAmount.id -> Seq(baseTxnScoreTrigger,
          updateTSO(baseTxnScoreTrigger,java.sql.Date.valueOf("2018-01-13"),Some(50)),
          updateTSO(baseTxnScoreTrigger,java.sql.Date.valueOf("2018-01-01"),Some(30))
        ))
    )
    val result = scoreToTest.score(threeScoresInOutcomePeriod)

    result shouldBe defined
    result.get.severity shouldBe Some(100)
    result.get.underlyingScores should have size 3
  }

  it should "not produce output if the inputs are outside the outcome period" in {
    val twoScoresOutsideOutcomePeriod:CustomerRollup[TransactionScoreOutput] = CustomerRollup(
      subject = "Cust001",
      keys = CustomerKey("Cust001"),
      customScoreOutputMap = Map(
        TransactionIsRoundAmount.id -> Seq(
          updateTSO(baseTxnScoreTrigger,java.sql.Date.valueOf("2017-12-30"),Some(50)),
          updateTSO(baseTxnScoreTrigger,java.sql.Date.valueOf("2017-12-29"),Some(30))
        ))
    )
    val result = scoreToTest.score(twoScoresOutsideOutcomePeriod)

    result should not be defined
  }

  it should "not produce output if there are no inputs for this score" in {
    val differentScoreTriggers:CustomerRollup[TransactionScoreOutput] = CustomerRollup(
      subject = "Cust001",
      keys = CustomerKey("Cust001"),
      customScoreOutputMap = Map(
        "SomeOtherScoreID" -> Seq(
          updateTSO(baseTxnScoreTrigger,java.sql.Date.valueOf("2017-12-31"),Some(50)),
          updateTSO(baseTxnScoreTrigger,java.sql.Date.valueOf("2017-12-30"),Some(30))
        ))
    )
    val result = scoreToTest.score(differentScoreTriggers)

    result should not be defined
  }

  private def updateTSO(tso:TransactionScoreOutput, dt:java.sql.Date) = tso.copy(keys=tso.keys.copy(analysisDate = dt))
  private def updateTSO(tso:TransactionScoreOutput, dt:java.sql.Date, severity:Option[Int]) = tso.copy(keys=tso.keys.copy(analysisDate = dt),severity = severity)
}
