package com.quantexa.example.scoring.scores.fiu.entity

import com.quantexa.example.scoring.scores.fiu.testdata.EntityTestData.testPhone
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class PhoneHitTest extends FlatSpec with Matchers {
  "PhoneHitTest" should "create a score" in {
    val testPhoneScore = PhoneHit.score(testPhone)(ScoreInput.empty)
    testPhoneScore.get.severity.get shouldBe 10
  }
}