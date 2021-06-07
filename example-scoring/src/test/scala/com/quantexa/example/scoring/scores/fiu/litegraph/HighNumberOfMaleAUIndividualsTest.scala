package com.quantexa.example.scoring.scores.fiu.litegraph

import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.example.scoring.scores.fiu.testdata.LiteGraphTestData
import com.quantexa.scoring.framework.parameters.{IntegerScoreParameter, ParameterIdentifier}
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HighNumberOfMaleAUIndividualsTest extends FlatSpec with Matchers {

  implicit val scoreInputWithParameter = ScoreInput.empty.copy(parameters = Map(
    ParameterIdentifier(None, "NumberMaleAUCustomersThreshold") -> IntegerScoreParameter("NumberMaleAUCustomersThreshold", "test", 2)
  ))

   "HighNumberOfMaleAUIndividuals" should "score networks with high number of male AU individuals with high severity" in {
     val testData = LiteGraphTestData.getOrElse("highNumberOfAUMaleIndividuals")
    val testHighNumberOfMaleAUIndividuals = HighNumberOfMaleAUIndividuals.score(
      testData
    )
    testHighNumberOfMaleAUIndividuals.get.severity.get shouldBe 100
   }

  it should "not create score output for networks with small number of male AU individuals" in {
    val testData = LiteGraphTestData.getOrElse("smallNumberOfAUMaleIndividuals")
    val testLowNumberOfMaleAUIndividuals = HighNumberOfMaleAUIndividuals.score(
      testData
    )
    testLowNumberOfMaleAUIndividuals shouldBe None
  }

  it should "not create score output for networks with no male AU individuals" in {
    val testData = LiteGraphTestData.getOrElse("simpleCustomerNetwork")
    val testNoHits = HighNumberOfMaleAUIndividuals.score(
      testData
    )
    testNoHits shouldBe None
  }
}