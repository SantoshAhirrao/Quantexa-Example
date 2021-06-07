package com.quantexa.example.scoring.scores.fiu.entity

import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import com.quantexa.example.scoring.scores.fiu.testdata.EntityTestData.testIndividualAtts
import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.testHotlist

@RunWith(classOf[JUnitRunner])
class IndividualOnHotlistTest extends FlatSpec with Matchers {
  
  val scoreSeverity = 100
  
  "IndividualOnHotlist" should "score individuals on hotlist with high severity" in {
    val testHotlistIndividuals = IndividualOnHotlist.score(
      testIndividualAtts,
      Seq(
        testHotlist.copy(hotlistId = 1L),
        testHotlist.copy(hotlistId = 2L),
        testHotlist.copy(hotlistId = 3L))
    )(ScoreInput.empty)
    getSeverity(testHotlistIndividuals) shouldBe scoreSeverity
  }

  it should "not create score output for individuals not on hotlist" in {  
    val testNonHotlistIndividuals = IndividualOnHotlist.score(
      testIndividualAtts,
      Seq.empty
    )(ScoreInput.empty)
    testNonHotlistIndividuals shouldBe None
  }
  
  private def getSeverity(scoreOutput: Option[BasicScoreOutput]): Int = {
    if (scoreOutput.isDefined && scoreOutput.get.severity.isDefined) {
      scoreOutput.get.severity.get
    } else -999
  }

}