package com.quantexa.example.scoring.scores.fiu.entity

import com.quantexa.model.core.datasets.ParsedDatasets.LensParsedIndividualName
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.utils.DateParsing
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import com.quantexa.example.scoring.scores.fiu.testdata.EntityTestData.testIndividualAtts
import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData

@RunWith(classOf[JUnitRunner])
class IndividualWithIDManipulationTest extends FlatSpec with Matchers {
  def mapCustomerDispName(parsedName: Seq[LensParsedIndividualName[Customer]], str: String) =
    parsedName.map(name => name.copy(nameDisplay = name.nameDisplay.map(a => str)).asInstanceOf[LensParsedIndividualName[Customer]])

  val mainCustomer = DocumentTestData.testCustomer

  val docSequence1 = Seq(
    mainCustomer.copy(dateOfBirth = Some(DateParsing.parseDate("2016/01/01"))),
    mainCustomer.copy(dateOfBirth = Some(DateParsing.parseDate("2014/01/01")))
  )

  val docSequence2 = Seq(
    mainCustomer.copy(personalNationalIdNumber = Some(5678.toLong)),
    mainCustomer.copy(personalNationalIdNumber = Some(5687.toLong))
  )

  val docSequence3 = Seq(
    mainCustomer.copy(parsedCustomerName = mapCustomerDispName(mainCustomer.parsedCustomerName, "test5") ),
    mainCustomer.copy(parsedCustomerName = mapCustomerDispName(mainCustomer.parsedCustomerName, "test6") )
  )

  val docSequence4 = Seq(
    mainCustomer.copy(),
    mainCustomer.copy()
  )

  "IndividualWithIDManipulation" should "score customers with multiple dates of birth " in {
    val testHighRisk = IndividualWithIDManipulation.score(testIndividualAtts, docSequence1)(ScoreInput.empty)
    testHighRisk.get.severity.get  shouldBe 100
  }

  it should "score customers with multiple personal IDs " in {
    val testHighRisk = IndividualWithIDManipulation.score(testIndividualAtts, docSequence2)(ScoreInput.empty)
    testHighRisk.get.severity.get  shouldBe 100
  }

  it should "score customers with multiple names" in {
    val testHighRisk = IndividualWithIDManipulation.score(testIndividualAtts, docSequence3)(ScoreInput.empty)
    testHighRisk.get.severity.get  shouldBe 100
  }

  it should "not score customers that are the same" in {
    val testHighRisk = IndividualWithIDManipulation.score(testIndividualAtts, docSequence4)(ScoreInput.empty)
    testHighRisk shouldBe None
  }
}