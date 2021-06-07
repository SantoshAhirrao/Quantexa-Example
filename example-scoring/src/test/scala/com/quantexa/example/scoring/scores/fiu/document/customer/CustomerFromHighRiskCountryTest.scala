package com.quantexa.example.scoring.scores.fiu.document.customer

import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.{testCustomer, testRiskCountryCodeLookup}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CustomerFromHighRiskCountryTest extends FlatSpec with Matchers with OptionValues {
  
   val riskCountryCodes = Seq("BB", "JO", "SY")

   val MaximumScoreSeverity = 100

   "CustomerFromRiskCountry" should "score customers resident in a risk country with high severity" in {
    val testHighRiskCountry = CustomerFromHighRiskCountry.score(testCustomer.copy(residenceCountry = Some("SY")), testRiskCountryCodeLookup)(ScoreInput.empty)
    val testSeverity = testHighRiskCountry.flatMap(_.severity)
    testSeverity.value shouldBe MaximumScoreSeverity
   }

  it should "not create score output for customers not resident in a risk country" in {
    val testNonHighRiskCountry = CustomerFromHighRiskCountry.score(testCustomer.copy(residenceCountry = Some("ES")), testRiskCountryCodeLookup)(ScoreInput.empty)
    testNonHighRiskCountry shouldBe None
  }

  it should "not create score output for customers with an empty residence country" in {
    val testNonResidenceCountry = CustomerFromHighRiskCountry.score(testCustomer.copy(residenceCountry = None), testRiskCountryCodeLookup)(ScoreInput.empty)
    testNonResidenceCountry shouldBe None
  }
}
