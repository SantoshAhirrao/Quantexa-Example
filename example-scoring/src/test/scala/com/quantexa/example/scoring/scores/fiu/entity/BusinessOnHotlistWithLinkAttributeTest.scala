package com.quantexa.example.scoring.scores.fiu.entity

import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import com.quantexa.example.scoring.scores.fiu.testdata.EntityTestData.testBusiness
import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.testHotlist

@RunWith(classOf[JUnitRunner])
class BusinessOnHotlistWithLinkAttributeTest extends FlatSpec with Matchers {
  
  val scoreSeverity = 100
  
  "BusinessOnHotlistWithLinkAttribute" should "score businesses with link to hotlist with high severity" in {
    val testHotlistBusinesses = BusinessOnHotlist.score(
      testBusiness,
      Seq(
        testHotlist.copy(hotlistId = 1L),
        testHotlist.copy(hotlistId = 2L),
        testHotlist.copy(hotlistId = 3L))
    )(ScoreInput.empty)
    getSeverity(testHotlistBusinesses) shouldBe scoreSeverity
  }
  
  private def getSeverity(scoreOutput: Option[BasicScoreOutput]): Int = {
    if (scoreOutput.isDefined && scoreOutput.get.severity.isDefined) {
      scoreOutput.get.severity.get
    } else -999
  }

}