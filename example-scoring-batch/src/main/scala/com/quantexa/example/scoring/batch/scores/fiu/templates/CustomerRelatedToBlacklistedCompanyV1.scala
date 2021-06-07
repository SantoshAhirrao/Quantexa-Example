package com.quantexa.example.scoring.batch.scores.fiu.templates

import com.quantexa.resolver.core.EntityGraphLite._
import com.quantexa.analytics.scala.scoring.model.ScoringModel._
import com.quantexa.analytics.scala.graph.LiteGraphUtils._
import com.quantexa.resolver.core.EntityGraph.DocumentId
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.LiteGraphScore

case class NetworkKey(graphSubjectId: DocumentId) extends GraphSubjectIdKey
/**
  * Customer Related to Blacklisted Company is interpreted as follows:
  * The customer's social network is defined as all entities who are connected to the customer via non trade/transactional links.
  * If there is a hotlisted business who is in the customer's social network (and is not the customer themselves) then the rule fires.
  *
  * [[CustomerRelatedToBlacklistedCompanyV1]] is the preferred version of this rule, and demonstrates good use of for comprehensions.
  *
  */
object CustomerRelatedToBlacklistedCompanyV1 extends LiteGraphScore[LiteGraphScoreOutput[NetworkKey]] {
  def id = "CustomerRelatedToBlacklistedCompanyV1"
  def score(liteGraphWithId: LiteGraphWithId)(implicit scoreInput: ScoreInput): Option[LiteGraphScoreOutput[NetworkKey]] = {

    val liteGraph = liteGraphWithId.graph
    val graphSubjectId = liteGraphWithId.graphSubjectId.getOrElse(
      throw new IllegalStateException("Graph built with graph scripting must have subject id.")
    )

    val hotListDocsForCustomer = getHotlistsDocumentsForCustomer(liteGraph, graphSubjectId)

    val liteGraphWithoutCustHotlistDoc = removeHotlistsForCustomerFromLiteGraph(liteGraph, hotListDocsForCustomer)

    if (liteGraphWithoutCustHotlistDoc.networkContainsDocumentType("hotlist")) {

      val liteGraphNoTransactions: LiteGraph = liteGraphWithoutCustHotlistDoc.deleteDocumentsByType(Set("trade", "transaction"))
      val connectedComponents = liteGraphNoTransactions.splitGraph //Need to implement splitGraph on LiteGraph

      val (focalCustomerSocialNetwork, otherNetworks) = partitionLiteGraphsBySubject(connectedComponents, graphSubjectId)

      val hotListDocumentsInNetwork = focalCustomerSocialNetwork.getDocumentsByType("hotlist")

      if (hotListDocumentsInNetwork.nonEmpty) {
        val hotlistedEntities = getBlacklistedEntitiesRelatedToCustomer(focalCustomerSocialNetwork, hotListDocumentsInNetwork)

        val hotlistedEntityLabels = hotlistedEntities.map(e => e.label)
        Some(new LiteGraphScoreOutput(
          keys = NetworkKey(graphSubjectId),
          severity = Some(100),
          band = None,
          description = Some(s"""The following customers are blacklisted and connected to focal customer: ${hotlistedEntityLabels.mkString(",")}"""),
          underlyingScores = Seq.empty
        ))
      } else None
    } else None
  }

  private def getBlacklistedEntitiesRelatedToCustomer(focalCustomerSocialNetwork: LiteGraph, hotListDocumentsInNetwork: Seq[LiteDocument]) = {
    for {
      hotlistDoc <- hotListDocumentsInNetwork
      hotlistedEntities <- focalCustomerSocialNetwork.getConnectedNodes(hotlistDoc) if hotlistedEntities.entityType == "business"
    } yield hotlistedEntities
  }

  private def getHotlistsDocumentsForCustomer(liteGraph: LiteGraph, graphSubjectId: DocumentId) = {
    for {
      focalCustomerEntity <- liteGraph.getConnectedNodes(graphSubjectId) if attributesContainAllSpecifiedValues(focalCustomerEntity.attributes, Map("role" -> "customer")) && focalCustomerEntity.entityType == "business"
      hotlistConnectedToFocalCustomer <- liteGraph.getConnectedNodes(focalCustomerEntity) if hotlistConnectedToFocalCustomer.documentType == "hotlist"
    } yield hotlistConnectedToFocalCustomer
  }

  private def removeHotlistsForCustomerFromLiteGraph(liteGraph: LiteGraph, hotListDocsForCustomer: Seq[LiteDocument]) = {
    liteGraph.deleteDocumentsById(hotListDocsForCustomer.map(_.documentId).toSet)
  }

  def partitionLiteGraphsBySubject(liteGraphComponents: Seq[LiteGraph], graphSubjectId: DocumentId): (LiteGraph, Seq[LiteGraph]) = {
    val (focalCustomerSocialNetwork :: Nil, otherNetworks) = liteGraphComponents.partition (lg => lg.docs.exists (d => d.documentId == graphSubjectId) )
    (focalCustomerSocialNetwork, otherNetworks)
  }
}


