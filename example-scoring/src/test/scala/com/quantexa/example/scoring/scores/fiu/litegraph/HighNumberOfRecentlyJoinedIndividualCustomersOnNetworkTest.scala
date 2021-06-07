package com.quantexa.example.scoring.scores.fiu.litegraph

import com.quantexa.example.scoring.scores.fiu.testdata.LiteGraphTestData
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{IntegerScoreParameter, StringScoreParameter, ParameterIdentifier}
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HighNumberOfRecentlyJoinedIndividualCustomersOnNetworkTest extends FlatSpec with Matchers with OptionValues{

  implicit val ScoreInputWithParameter = ScoreInput.empty.copy(parameters = Map(
    ParameterIdentifier(None, "NumberRecentlyJoinedIndividualCustomersThreshold") -> IntegerScoreParameter("NumberRecentlyJoinedIndividualCustomersThreshold", "test", 2),
    ParameterIdentifier(None, "ThresholdJoinDate") -> StringScoreParameter("ThresholdJoinDate", "test", "2017/01/01"))
  )

   "HighNumberOfRecentlyJoinedIndividualCustomersOnNetwork" should "score networks with high number of recently joined individual customers with high severity" in {
     val testData = LiteGraphTestData.getOrElse("highNumberOfRecentlyJoinedIndividualCustomers")
    val testHighNumberOfRecentlyJoinedIndividualCustomersOnNetwork = HighNumberOfRecentlyJoinedIndividualCustomersOnNetwork.score(
      testData
    )
    testHighNumberOfRecentlyJoinedIndividualCustomersOnNetwork.flatMap(_.severity).value shouldBe 100
   }

  it should "not create score output for networks with small number of recently joined individual customers" in {
    val testData = LiteGraphTestData.getOrElse("smallNumberOfRecentlyJoinedIndividualCustomers")
    val testLowNumberOfRecentlyJoinedIndividualCustomersOnNetwork = HighNumberOfRecentlyJoinedIndividualCustomersOnNetwork.score(
      testData
    )
    testLowNumberOfRecentlyJoinedIndividualCustomersOnNetwork shouldBe None
  }

  it should "not create score output for networks with no recently joined individual customers" in {
    val testData = LiteGraphTestData.getOrElse("simpleCustomerNetwork")
    val testNoRecentlyJoinedIndividualCustomers = HighNumberOfRecentlyJoinedIndividualCustomersOnNetwork.score(
      testData
    )
    testNoRecentlyJoinedIndividualCustomers shouldBe None
  }
}