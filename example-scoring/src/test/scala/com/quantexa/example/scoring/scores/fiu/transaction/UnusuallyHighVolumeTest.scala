package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData.testCustomerDateFacts
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{DoubleScoreParameter, ParameterIdentifier}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class UnusuallyHighVolumeTest extends FlatSpec with Matchers {

  val MaxSeverity = 100

  val unusualVolumeScores = Set(UnusuallyHigh1DayVolume, UnusuallyHigh7DaysVolume, UnusuallyHigh30DaysVolume)

  implicit val ScoreInputWithParameter = ScoreInput.empty.copy(parameters = Map(
    ParameterIdentifier(None, "UnusuallyHigh1DayTransactionVolume_numberOfStdDevsUnusualMediumSeverity") -> DoubleScoreParameter("UnusuallyHigh1DayTransactionVolume_numberOfStdDevsUnusualMediumSeverity", "test", 2D),
    ParameterIdentifier(None, "UnusuallyHigh7DaysTransactionVolume_numberOfStdDevsUnusualMediumSeverity") -> DoubleScoreParameter("UnusuallyHigh7DaysTransactionVolume_numberOfStdDevsUnusualMediumSeverity", "test", 2D),
    ParameterIdentifier(None, "UnusuallyHigh30DaysTransactionVolume_numberOfStdDevsUnusualMediumSeverity") -> DoubleScoreParameter("UnusuallyHigh30DaysTransactionVolume_numberOfStdDevsUnusualMediumSeverity", "test", 2D),
    ParameterIdentifier(None, "UnusuallyHigh1DayTransactionVolume_numberOfStdDevsUnusualHighSeverity") -> DoubleScoreParameter("UnusuallyHigh1DayTransactionVolume_numberOfStdDevsUnusualHighSeverity", "test", 2.3D),
    ParameterIdentifier(None, "UnusuallyHigh7DaysTransactionVolume_numberOfStdDevsUnusualHighSeverity") -> DoubleScoreParameter("UnusuallyHigh7DaysTransactionVolume_numberOfStdDevsUnusualHighSeverity", "test", 2.3D),
    ParameterIdentifier(None, "UnusuallyHigh30DaysTransactionVolume_numberOfStdDevsUnusualHighSeverity") -> DoubleScoreParameter("UnusuallyHigh30DaysTransactionVolume_numberOfStdDevsUnusualHighSeverity", "test", 2.3D)
  ))

  private def copyIntoTotalValueField(scr:UnusuallyHighVolume, volume:Long) = scr match {
    case UnusuallyHigh1DayVolume => testCustomerDateFacts.copy(numberOfTransactionsToday = volume)
    case UnusuallyHigh7DaysVolume => testCustomerDateFacts.copy(numberOfTransactionsOverLast7Days = volume)
    case UnusuallyHigh30DaysVolume => testCustomerDateFacts.copy(numberOfTransactionsOverLast30Days = volume)
  }

  unusualVolumeScores.foreach {
    score =>
      behavior of score.id
      it should "trigger on a large periods worth (by volume) of transactions" in {
        val actualScoreResult = score
          .score(customerDate = copyIntoTotalValueField(score,10000))
        actualScoreResult.flatMap(_.severity).getOrElse(-1) shouldBe MaxSeverity
      }
      it should "not trigger on a low periods worth (by volume) of transactions" in {
        val testHighValue = score
          .score(customerDate = copyIntoTotalValueField(score, 10))
        testHighValue shouldBe None
      }
  }

}