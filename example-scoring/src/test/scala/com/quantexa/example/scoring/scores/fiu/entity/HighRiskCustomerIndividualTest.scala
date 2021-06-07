package com.quantexa.example.scoring.scores.fiu.entity

import com.quantexa.example.model.fiu.scoring.EntityAttributes.Individual
import com.quantexa.example.scoring.utils.DateParsing
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import com.quantexa.example.scoring.scores.fiu.testdata.EntityTestData.testIndividualAtts
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, IntegerScoreParameter}

@RunWith(classOf[JUnitRunner])
class HighRiskCustomerIndividualTest extends FlatSpec with Matchers {

  implicit val ScoreInputWithParameter = ScoreInput.empty.copy(parameters = Map(
    ParameterIdentifier(None, "HighRiskCustomerIndividual_riskRatingThreshold") -> IntegerScoreParameter("HighRiskCustomerIndividual_riskRatingThreshold", "test", 4500)
  ))

  "HighRiskCustomerIndividual" should "score individuals with a high risk value with high severity" in {
    val testHighRisk = HighRiskCustomerIndividual.score(testIndividualAtts.copy(customerRiskRating = Some(10000)))
    testHighRisk.get.severity.get shouldBe 100
  }

  it should "not create score output for individuals with an empty risk value" in {
    val testMissingRisk = HighRiskCustomerIndividual.score(testIndividualAtts.copy(customerRiskRating = None))
    testMissingRisk shouldBe None
  }
}