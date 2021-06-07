package com.quantexa.example.scoring.batch.scores.fiu.templates

import com.quantexa.example.scoring.batch.utils.fiu.LiteGraphTestData
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LiteGraphScoreTests extends FlatSpec with Matchers with OptionValues{

  /**
    * Documentation on the network structure of each test case can be found at https://quantexa.atlassian.net/wiki/spaces/TECH/pages/804651158/Scoring+Tests
    */

  implicit val scoreInput = ScoreInput.empty

  behavior of "Customer Settling To Related Third Parties via Counterparty Businesses"

// V1 of score

  it should "trigger for customer settling to counterparties related via BvD" in {
    // The score should trigger where two third party business entities that the subject is trading with are also
    // linked via BvD
    val result = CustomerSettlingToRelatedThirdPartiesBusiness.score(
      LiteGraphTestData.data("customerSettlingRelatedCounterparties"))
    assert(result.value.underlyingScores.length == 2)
  }

  it should "trigger for customer settling to multiple counterparties, a subset of which are linked via BvD" in {
    // A subset of the third parties on the network are related via BvD, the score should trigger
    val result = CustomerSettlingToRelatedThirdPartiesBusiness.score(
      LiteGraphTestData.data("subsetOfCounterpartiesRelated"))
    assert(result.isDefined)
  }

  it should "not trigger for customer with trade relationship documents that can be directly linked via the counterparty business entity" in {
    // Trades linked by the same business entity should not trigger the score
    val result = CustomerSettlingToRelatedThirdPartiesBusiness.score(
      LiteGraphTestData.data("settlingToSameBusinessViaMultipleRelationships"))
    assert(result.isEmpty)
  }

   it should "not trigger for customer's trade relationship if it links back to the original customer via a social connection" in {
     // For related third parties, the score should not trigger if the sub graph that they occupy is shared with the
     // graph's subject
     val result = CustomerSettlingToRelatedThirdPartiesBusiness.score(
       LiteGraphTestData.data("tradesLinkedBackToCustomer"))
     assert(result.isDefined)
     assert(result.get.description.contains("The subject of the graph is trading with linked third parties: Linked set - cp9_3, cp9_4."))
   }

  behavior of "Customer Settling To Related Third Parties via Counterparty Businesses V2"

  it should "trigger for customer settling to counterparties related via BvD" in {
    // The score should trigger where two third party business entities that the subject is trading with are also
    // linked via BvD
    val result = CustomerSettlingToRelatedThirdPartiesBusinessV2.score(
      LiteGraphTestData.data("customerSettlingRelatedCounterparties"))
    assert(result.isDefined)
  }

  it should "trigger for customer settling to multiple counterparties, a subset of which are linked via BvD" in {
    // A subset of the third parties on the network are related via BvD, the score should trigger
    val result = CustomerSettlingToRelatedThirdPartiesBusinessV2.score(
      LiteGraphTestData.data("subsetOfCounterpartiesRelated"))
    assert(result.isDefined)
  }

  it should "not trigger for customer with trade relationship documents that can be directly linked via the counterparty business entity" in {
    // Trades linked by the same business entity should not trigger the score
    val result = CustomerSettlingToRelatedThirdPartiesBusinessV2.score(
      LiteGraphTestData.data("settlingToSameBusinessViaMultipleRelationships"))
    assert(result.isEmpty)
  }

  it should "not trigger for customer's trade relationship if it links back to the original customer via a social connection" in {
    // For related third parties, the score should not trigger if the sub graph that they occupy is shared with the
    // graph's subject
    val result = CustomerSettlingToRelatedThirdPartiesBusinessV2.score(
      LiteGraphTestData.data("tradesLinkedBackToCustomer"))
    assert(result.isDefined)
    assert(result.get.description.contains("The subject of the graph is trading with linked third parties: Linked set - cp9_3, cp9_4."))
  }

    val result = CustomerSettlingToRelatedThirdPartiesAddress.score(
      LiteGraphTestData.data("relatedCounterpartiesLinkedByAccount"))
    assert(result.isEmpty)
  behavior of "Customer Settling To Related Third Parties via Address - Address Score"

  it should "trigger for customer where trade relationships share a common address entity" in {
    // The score should trigger where two trades are linked via an address entity only
    val result = CustomerSettlingToRelatedThirdPartiesAddress.score(
      LiteGraphTestData.data("customerTradeRelationshipsCommonAddress"))
    assert(result.isDefined)
  }

  it should "not trigger for where counterparties linked by an address share the same account" in {
    // if multiple trades are linked by an address but they are also linked by a business or an account entity then
    // this score should not fire
    val result = CustomerSettlingToRelatedThirdPartiesAddress.score(
      LiteGraphTestData.data("relatedCounterpartiesLinkedByAccount"))
    assert(result.isEmpty)
  }
}