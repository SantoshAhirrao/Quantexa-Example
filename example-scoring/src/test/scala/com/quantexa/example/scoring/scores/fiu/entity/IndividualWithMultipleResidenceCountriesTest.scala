package com.quantexa.example.scoring.scores.fiu.entity

import com.quantexa.example.model.fiu.scoring.EntityAttributes.Individual
import com.quantexa.example.scoring.utils.DateParsing
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.example.model.fiu.scoring.DocumentAttributes.CustomerAttributes
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import com.quantexa.example.scoring.scores.fiu.testdata.EntityTestData.testIndividualAtts
import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.testCustomerAtts

@RunWith(classOf[JUnitRunner])
class IndividualWithMultipleResidenceCountriesTest extends FlatSpec with Matchers {
  "IndividualWithMultipleResidenceCountries" should "score individuals with a high number of residence countries with high severity" in {
    val testMultipleCountries = IndividualWithMultipleResidenceCountries.score(
      testIndividualAtts,
      Seq(
        testCustomerAtts.copy(residenceCountry = Some("US")),
        testCustomerAtts.copy(residenceCountry = Some("US")),
        testCustomerAtts.copy(residenceCountry = Some("UK")))
    )(ScoreInput.empty)
    testMultipleCountries.get.severity.get shouldBe 100
  }

  it should "not create score output for individuals with a single repeated residence country" in {  
    val testSingleRepeatedCountry = IndividualWithMultipleResidenceCountries.score(
      testIndividualAtts,
      Seq(
        testCustomerAtts.copy(residenceCountry = Some("US")),
        testCustomerAtts.copy(residenceCountry = Some("US")),
        testCustomerAtts.copy(residenceCountry = Some("US")))
    )(ScoreInput.empty)
    testSingleRepeatedCountry shouldBe None
  }

  it should "not count empty country strings" in { 
    val testMissingCountry = IndividualWithMultipleResidenceCountries.score(
      testIndividualAtts,
      Seq(
        testCustomerAtts.copy(residenceCountry = Some("US")),
        testCustomerAtts.copy(residenceCountry = Some("US")),
        testCustomerAtts.copy(residenceCountry = None))
    )(ScoreInput.empty)
    testMissingCountry shouldBe None
  }
}