package com.quantexa.example.scoring.scores.fiu.document.customer

import com.quantexa.example.scoring.model.fiu.FiuModelDefinition
import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.testCustomer
import com.quantexa.example.scoring.utils.DateParsing
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, DateScoreParameter}
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NewCustomerTest extends FlatSpec with Matchers {
  val oldStartDate = Some(DateParsing.parseDate("2004/01/01"))
  val recentStartDate = Some(DateParsing.parseDate("2017/01/01"))
  val missingStartDate = Some(DateParsing.parseDateOrDefault(""))

  implicit val ScoreInputWithParameter = ScoreInput.empty.copy(parameters = Map(
    ParameterIdentifier(None, "NewCustomer_newDateThreshold") -> DateScoreParameter("NewCustomer_newDateThreshold", "test", java.sql.Date.valueOf("2006-01-01")))
  )

  "NewCustomer" should "not create score output for customers that are not new" in {
    val testOldDate = NewCustomer.score(testCustomer.copy(customerStartDate = oldStartDate))
    testOldDate shouldBe None
  }

  it should "score customers that are new with high severity" in {
    val testNewDate = NewCustomer.score(testCustomer.copy(customerStartDate = recentStartDate))
    testNewDate.get.severity.get shouldBe 100
  }

  it should "not create score output for customers with an empty risk value" in {
    val testMissingDate = NewCustomer.score(testCustomer.copy(customerStartDate = missingStartDate))
    testMissingDate shouldBe None
  }
}