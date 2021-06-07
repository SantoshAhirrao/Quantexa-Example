package com.quantexa.example.scoring.scores.fiu.document.customer

import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.testCustomer
import com.quantexa.example.scoring.utils.DateParsing
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HighRiskCustomerContinuousTest extends FlatSpec with Matchers with OptionValues{
  val aDate = Some(DateParsing.parseDate("2016/01/01"))

   "HighRiskCustomer" should "score customers with a high risk value with high severity" in {
    val testHighRisk = HighRiskCustomerContinuous.score(testCustomer.copy(customerRiskRating = Some(9028)))(ScoreInput.empty)
    testHighRisk.flatMap(_.severity).value shouldBe 100
  }

  "MediumRiskCustomer" should "score customers with a high risk value with high severity" in {
    val testHighRisk = HighRiskCustomerContinuous.score(testCustomer.copy(customerRiskRating = Some(7500)))(ScoreInput.empty)
    testHighRisk.flatMap(_.severity).value shouldBe 56
  }

  it should "not create score output for customers with a low risk value" in {
    val testLowRisk = HighRiskCustomerContinuous.score(testCustomer.copy(customerRiskRating = Some(0)))(ScoreInput.empty)
    testLowRisk shouldBe None
  }

  it should "not create score output for customers with an empty risk value" in {
    val testNoRisk = HighRiskCustomerContinuous.score(testCustomer.copy(customerRiskRating = None))(ScoreInput.empty)
    testNoRisk shouldBe None
  }
}