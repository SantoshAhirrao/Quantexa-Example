package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.example.scoring.scores.fiu.testdata.FactsTestData.testCustomerDateFacts
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{DoubleScoreParameter, ParameterIdentifier}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class UnusuallyHighValueForCustomerTest extends FlatSpec with Matchers {
  val MaxSeverity = 100

  val unusualVolumeScores = Set(UnusuallyHigh1DayValueForCustomer,UnusuallyHigh7DaysValueForCustomer,UnusuallyHigh30DaysValueForCustomer)

  implicit val ScoreInputWithParameter = ScoreInput.empty.copy(parameters = Map(
    ParameterIdentifier(None, "UnusuallyHigh1DayValueForCustomer_numberOfStdDevsUnusualMediumSeverity") -> DoubleScoreParameter("UnusuallyHigh1DayValueForCustomer_numberOfStdDevsUnusualMediumSeverity", "test", 2D),
    ParameterIdentifier(None, "UnusuallyHigh7DaysValueForCustomer_numberOfStdDevsUnusualMediumSeverity") -> DoubleScoreParameter("UnusuallyHigh7DaysValueForCustomer_numberOfStdDevsUnusualMediumSeverity", "test", 2D),
    ParameterIdentifier(None, "UnusuallyHigh30DaysValueForCustomer_numberOfStdDevsUnusualMediumSeverity") -> DoubleScoreParameter("UnusuallyHigh30DaysValueForCustomer_numberOfStdDevsUnusualMediumSeverity", "test", 2D),
    ParameterIdentifier(None, "UnusuallyHigh1DayValueForCustomer_numberOfStdDevsUnusualHighSeverity") -> DoubleScoreParameter("UnusuallyHigh1DayValueForCustomer_numberOfStdDevsUnusualHighSeverity", "test", 2.3D),
    ParameterIdentifier(None, "UnusuallyHigh7DaysValueForCustomer_numberOfStdDevsUnusualHighSeverity") -> DoubleScoreParameter("UnusuallyHigh7DaysValueForCustomer_numberOfStdDevsUnusualHighSeverity", "test", 2.3D),
    ParameterIdentifier(None, "UnusuallyHigh30DaysValueForCustomer_numberOfStdDevsUnusualHighSeverity") -> DoubleScoreParameter("UnusuallyHigh30DaysValueForCustomer_numberOfStdDevsUnusualHighSeverity", "test", 2.3D)

  ))

  private def copyIntoTotalValueField(scr:UnusuallyHighValueForCustomer, amount:Double) = scr match {
    case UnusuallyHigh1DayValueForCustomer => testCustomerDateFacts.copy(totalValueTransactionsToday = amount)
    case UnusuallyHigh7DaysValueForCustomer =>testCustomerDateFacts.copy(totalValueTransactionsOverLast7Days = amount)
    case UnusuallyHigh30DaysValueForCustomer =>testCustomerDateFacts.copy(totalValueTransactionsOverLast30Days = amount)
  }

  unusualVolumeScores.foreach {
    score =>
      behavior of score.id
      it should "trigger on a large periods worth (by value) of transactions" in {
        val actualScoreResult = score
          .score(customerDate = copyIntoTotalValueField(score,10000.0))
        actualScoreResult.flatMap(_.severity).getOrElse(-1) shouldBe MaxSeverity
      }
      it should "not trigger on a low periods worth (by value) of transactions" in {
        val testHighValue = score
          .score(customerDate = copyIntoTotalValueField(score, 10.0))
        testHighValue shouldBe None
      }
  }
}
