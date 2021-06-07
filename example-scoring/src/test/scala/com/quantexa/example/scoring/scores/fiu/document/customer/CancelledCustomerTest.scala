package com.quantexa.example.scoring.scores.fiu.document.customer

import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.testCustomer
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CancelledCustomerTest extends FlatSpec with Matchers {

   "CancelledCustomer" should "score customers with a status of 'C' with high severity" in {
    val testIsCancelled = CancelledCustomer.score(testCustomer.copy(customerStatus = Some("C")))(ScoreInput.empty)
    testIsCancelled.get.severity.get shouldBe 100
   }

  it should "not create score output for customers with a status other than 'C' " in {
    val testNotCancelled = CancelledCustomer.score(testCustomer.copy(customerStatus = Some("A")))(ScoreInput.empty)
    testNotCancelled shouldBe None
  }

  it should "not create score output for customers with an empty status value" in {
    val testNoStatus = CancelledCustomer.score(testCustomer.copy(customerStatus = None))(ScoreInput.empty)
    testNoStatus shouldBe None
  }
}