package com.quantexa.example.scoring.scores.fiu.litegraph

import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.example.scoring.scores.fiu.testdata.LiteGraphTestData
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier,IntegerScoreParameter}
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HighNumberOfMaleUSIndividualsConnectedToHotlistTest extends FlatSpec with Matchers {

  implicit val ScoreInputWithParameter = ScoreInput.empty.copy(parameters = Map(
    ParameterIdentifier(None, "NumberHotlistMaleUSCustomersThreshold") -> IntegerScoreParameter("NumberHotlistMaleUSCustomersThreshold", "test", 2)
  ))

   "HighNumberOfMaleUSIndividualsConnectedToHotlist" should "score networks with high number of male US individuals on hotlist with high severity" in {
     val testData = LiteGraphTestData.getOrElse("highNumberOfUSMaleIndividuals")
    val testHighNumberOfMaleUSIndividualsConnectedToHotlist = HighNumberOfMaleUSIndividualsConnectedToHotlist.score(
      testData
    )
    testHighNumberOfMaleUSIndividualsConnectedToHotlist.get.severity.get shouldBe 100
   }

  it should "not create score output for networks with small number of male US individuals on hotlist" in {
    val testData = LiteGraphTestData.getOrElse("smallNumberOfUSMaleIndividuals")
    val testLowNumberOfMaleUSIndividualsConnectedToHotlist = HighNumberOfMaleUSIndividualsConnectedToHotlist.score(
      testData
    )
    testLowNumberOfMaleUSIndividualsConnectedToHotlist shouldBe None
  }

  it should "not create score output for networks with high number of male AU individuals on hotlist" in {
    val testData = LiteGraphTestData.getOrElse("highNumberOfAUMaleIndividuals")
    val testLowNumberOfMaleUSIndividualsConnectedToHotlist = HighNumberOfMaleUSIndividualsConnectedToHotlist.score(
      testData
    )
    testLowNumberOfMaleUSIndividualsConnectedToHotlist shouldBe None
  }

  it should "not create score output for networks with no male US individuals on hotlist" in {
    val testData = LiteGraphTestData.getOrElse("simpleCustomerNetwork")
    val testNoHotlistDocs = HighNumberOfMaleUSIndividualsConnectedToHotlist.score(
      testData
    )
    testNoHotlistDocs shouldBe None
  }
}