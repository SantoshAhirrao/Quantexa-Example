package com.quantexa.example.scoring.scores.fiu.document.customer

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import org.scalatest.junit.JUnitRunner
import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.{testCustomer, testPostcodePricesLookup}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.model.core.datasets.ParsedDatasets.{LensParsedAddress, ParsedAddress}
import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import com.quantexa.scoring.framework.parameters.{DoubleScoreParameter, ParameterIdentifier}
@RunWith(classOf[JUnitRunner])
class NonReputableAddressCustomerTest extends FlatSpec with Matchers with OptionValues{
  
  val customPostcodeMap: Map[String, Double] = Map("BB97SS" -> 50000.0, "SW72AZ" -> 2000000.0)

  val randomAddress: LensParsedAddress[Customer] = random[ParsedAddress].toParentVersion[Customer]

  val lowValueAddress: LensParsedAddress[Customer]  = randomAddress.copy(postcode = Some("BB97SS"))

  val highValueAddress: LensParsedAddress[Customer] = randomAddress.copy(postcode = Some("SW72AZ"))

  val MaximumScoreSeverity = 100

  implicit val ScoreInputWithParameter = ScoreInput.empty.copy(parameters = Map(
    ParameterIdentifier(None, "CustomerAddressInLowValuePostcode_PriceThreshold") -> DoubleScoreParameter("CustomerAddressInLowValuePostcode_PriceThreshold", "test", 80000.0D)
  ))

  "CustomerAddressInLowValuePostcode" should "score customers located in low value areas" in {
    val testNonReputableAddress = CustomerAddressInLowValuePostcode.score(
      testCustomer.copy(parsedAddress = Some(lowValueAddress)), testPostcodePricesLookup
    )
    testNonReputableAddress.flatMap(_.severity).value shouldEqual MaximumScoreSeverity
  }

  it should "not create score output for customers located in high value areas" in {
    val testReputableAddress = CustomerAddressInLowValuePostcode.score(
      testCustomer.copy(parsedAddress = Some(highValueAddress)), testPostcodePricesLookup.copy(price = 100000)
    )
    testReputableAddress shouldBe None
  }
     
}