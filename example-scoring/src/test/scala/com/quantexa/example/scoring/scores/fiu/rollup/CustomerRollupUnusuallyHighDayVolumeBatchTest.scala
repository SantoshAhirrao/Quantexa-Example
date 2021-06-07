package com.quantexa.example.scoring.scores.fiu.rollup

import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerDateScoreOutput, CustomerRollup}
import com.quantexa.example.scoring.model.fiu.ScoringModel._
import com.quantexa.example.scoring.scores.fiu.transaction.{UnusuallyHigh1DayVolume, UnusuallyHigh30DaysVolume, UnusuallyHigh7DaysValueForCustomer, UnusuallyHigh7DaysVolume}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{IntegerScoreParameter, ParameterIdentifier}
import org.scalatest.{FlatSpec, Matchers}

class CustomerRollupUnusuallyHighDayVolumeBatchTest extends FlatSpec with Matchers {

  val scoreToTest = CustomerRollupUnusuallyHighDayVolumeBatch(java.sql.Date.valueOf("2019-01-30"))
  implicit val outcomePeriod: ScoreInput = ScoreInput.empty.copy(
    parameters = Map(ParameterIdentifier(None, "outcomePeriodMonths") -> IntegerScoreParameter("outcomePeriodMonths", "Outcome Period in months", 1)))


  "CustomerRollupUnusuallyHighDayVolume" should "scale severity from 3 input scenario triggers in the outcome period" in {
    val threeScoresInOutcomePeriod: CustomerRollup[CustomerDateScoreOutput] = CustomerRollup(
      subject = "Cust001",
      keys = CustomerKey("Cust001"),
      customScoreOutputMap = CustomerRollupUnusuallyHighDayVolumeBatchTest.oneScoreTriggeredWith3Severities
    )
    val result = scoreToTest.score(threeScoresInOutcomePeriod)

    result shouldBe defined
    result.get.severity shouldBe Some((100 * 0.3).toInt)
  }

  it should "only score HighDayVolume scores in the outcome period" in {
    val twoRelevantScoresInOutputPeriod: CustomerRollup[CustomerDateScoreOutput] = CustomerRollup(
      subject = "Cust001",
      keys = CustomerKey("Cust001"),
      customScoreOutputMap = CustomerRollupUnusuallyHighDayVolumeBatchTest.twoVolumeScoresTriggered
    )
    val result = scoreToTest.score(twoRelevantScoresInOutputPeriod)

    result shouldBe defined
    result.get.severity shouldBe Some((60 * 0.53).toInt)
    result.get.underlyingScores should have size 2
  }

  it should "scale severities based on the number of scores triggered that day" in {

    val multipleSameDayScores: CustomerRollup[CustomerDateScoreOutput] = CustomerRollup(
      subject = "Cust001",
      keys = CustomerKey("Cust001"),
      customScoreOutputMap = CustomerRollupUnusuallyHighDayVolumeBatchTest.multipleVolumeScoresDifferentSeverities
    )

    val result = scoreToTest.score(multipleSameDayScores)
    result.get.severity shouldBe Some(60)
  }
}

object CustomerRollupUnusuallyHighDayVolumeBatchTest {
  val baseCustomerDateScoreTrigger = CustomerDateScoreOutput(
    keys = CustomerDateKey("Cust001", java.sql.Date.valueOf("2019-01-30")),
    severity = Some(100), band = None, description = None)

  val oneScoreTriggeredWith3Severities = Map(
    UnusuallyHigh1DayVolume.id -> Seq(baseCustomerDateScoreTrigger,
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-13"), Some(50)),
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-01"), Some(30))
    ))

  val twoVolumeScoresTriggered = Map(
    UnusuallyHigh7DaysValueForCustomer.id -> Seq(baseCustomerDateScoreTrigger,
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-13"), Some(90))
    ),
    UnusuallyHigh30DaysVolume.id -> Seq(
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-13"), Some(40))
    ),
    UnusuallyHigh1DayVolume.id -> Seq(
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-13"), Some(60)),
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2018-11-25"), Some(80))
    ))

  val multipleVolumeScoresDifferentSeverities = Map(
    UnusuallyHigh7DaysValueForCustomer.id -> Seq(baseCustomerDateScoreTrigger,
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-13"), Some(90))
    ),
    UnusuallyHigh30DaysVolume.id -> Seq(
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-11"), Some(50)),
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-12"), Some(90)),
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-13"), Some(80))
    ),
    UnusuallyHigh7DaysVolume.id -> Seq(
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-11"), Some(30)),
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-12"), Some(35)),
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-14"), Some(80)),
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-16"), Some(80))
    ),
    UnusuallyHigh1DayVolume.id -> Seq(
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-11"), Some(40)),
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-13"), Some(100)),
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-14"), Some(60)),
      updateCDSO(baseCustomerDateScoreTrigger, java.sql.Date.valueOf("2019-01-15"), Some(100))
    ))

  private def updateCDSO(dcso: CustomerDateScoreOutput, dt: java.sql.Date, severity: Option[Int]): CustomerDateScoreOutput
  = dcso.copy(keys = dcso.keys.copy(analysisDate = dt), severity = severity)

}