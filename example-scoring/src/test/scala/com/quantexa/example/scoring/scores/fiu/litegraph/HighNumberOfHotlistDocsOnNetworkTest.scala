package com.quantexa.example.scoring.scores.fiu.litegraph

import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.example.scoring.scores.fiu.testdata.LiteGraphTestData
import com.quantexa.scoring.framework.parameters.{IntegerScoreParameter, ParameterIdentifier}
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HighNumberOfHotlistDocsOnNetworkTest extends FlatSpec with Matchers {

  "HighNumberOfHotlistDocsOnNetwork" should "score networks with high number of hotlist docs with high severity" in {
    val testData = LiteGraphTestData.getOrElse("highNumberOfHotlists")
    val testHighNumberOfHotlistDocs = HighNumberOfHotlistDocsOnNetwork.score(
      testData
    )(ScoreInput.empty.copy(
      parameters = Map(
        ParameterIdentifier(None, "NumberHotlistDocsThreshold") -> IntegerScoreParameter("NumberHotlistDocsThreshold", "test", 2)
      )
    ))
    testHighNumberOfHotlistDocs.get.severity.get shouldBe 100
   }

  it should "not create score output for networks with small number of hotlist docs" in {
    val testData = LiteGraphTestData.getOrElse("smallNumberOfHotlists")
    val testLowNumberOfHotlistDocs = HighNumberOfHotlistDocsOnNetwork.score(
      testData
    )(ScoreInput.empty.copy(
      parameters = Map(
        ParameterIdentifier(None, "NumberHotlistDocsThreshold") -> IntegerScoreParameter("NumberHotlistDocsThreshold", "test", 2)
      )
    ))
    testLowNumberOfHotlistDocs shouldBe None
  }

  it should "not create score output for networks with no hotlist docs" in {
    val testData = LiteGraphTestData.getOrElse("simpleCustomerNetwork")
    val testNoHotlistDocs = HighNumberOfHotlistDocsOnNetwork.score(
      testData
    )(ScoreInput.empty.copy(
      parameters = Map(
        ParameterIdentifier(None, "NumberHotlistDocsThreshold") -> IntegerScoreParameter("NumberHotlistDocsThreshold", "test", 2)
      )
    ))
    testNoHotlistDocs shouldBe None
  }
}