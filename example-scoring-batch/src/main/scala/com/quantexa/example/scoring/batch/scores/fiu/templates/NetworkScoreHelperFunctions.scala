package com.quantexa.example.scoring.batch.scores.fiu.templates

import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityId}
import com.quantexa.resolver.core.EntityGraphLite.{LiteEntity, LiteGraph, LiteGraphWithId}

object NetworkScoreHelperFunctions {

  import com.quantexa.analytics.scala.graph.LiteGraphUtils._

  private def removeTradeLinksForSubjectEntities(liteGraph: LiteGraphWithId): LiteGraph = {
    val customerBusinessID = getSubjectBusinessEntityID(liteGraph)
    liteGraph.graph.copy(edges = liteGraph.edges.filter(e => !(customerBusinessID == e.entityId && e.documentType == "trade")))
  }

  def identifyTradesAndCounterpartiesForSubject(liteGraph: LiteGraphWithId) = {
    val subjectBusinessEntityID = getSubjectBusinessEntityID(liteGraph)
    val tradesForSubject = getTradesConnectedToEntities(liteGraph, subjectBusinessEntityID)
    val counterpartiesSubjectTradesWith = getCounterpartiesToSubject(liteGraph, subjectBusinessEntityID, tradesForSubject)
    (tradesForSubject, counterpartiesSubjectTradesWith)
  }

  def identifySocialNetworkComponents(liteGraph: LiteGraphWithId,
                                              removeNonSocialNodesAndEdges: LiteGraphWithId => LiteGraph = removeTradeLinksForSubjectEntities): (LiteGraph, Seq[LiteGraph]) = {
    val socialSubGraphs = removeNonSocialNodesAndEdges(liteGraph).splitGraph
    partitionLiteGraphsBySubject(socialSubGraphs, liteGraph.graphSubjectId.get)
  }

  def getSubjectBusinessEntityID(liteGraph: LiteGraphWithId): EntityId = {
    def businessIsSubject(entity: LiteEntity) = getAttributeValue[String]("role", entity.attributes).contains("subject")

    val businessSubject = liteGraph.graph.getConnectedNodes(liteGraph.graphSubjectId.get).filter { ent =>
      Set("business")(ent.entityType) && businessIsSubject(ent)
    }.map(_.entityId).headOption
    businessSubject.getOrElse(throw new IllegalArgumentException("The LiteGraph being scored does not have a subject business entity"))
  }

  private def partitionLiteGraphsBySubject(liteGraphComponents: Seq[LiteGraph], graphSubjectId: DocumentId): (LiteGraph, Seq[LiteGraph]) = {
    val (focalCustomerSocialNetwork :: Nil, otherNetworks) = liteGraphComponents.partition(lg => lg.docs.exists(d => d.documentId == graphSubjectId))
    (focalCustomerSocialNetwork, otherNetworks)
  }

  def getTradesConnectedToEntities(liteGraph: LiteGraphWithId, subjectBusinessID: EntityId): Set[DocumentId] = {
    (for {
      trade <- liteGraph.graph.getConnectedNodes(subjectBusinessID) if trade.documentType == "trade"
    } yield trade.documentId).toSet
  }

  def getCounterpartiesToSubject(liteGraph: LiteGraphWithId, subjectBusinessID: EntityId, customerTrades: Set[DocumentId]): Set[EntityId] = {
    {
      for {
        trade <- customerTrades
        counterparty <- liteGraph.graph.getConnectedNodes(trade) if subjectBusinessID != counterparty.entityId && counterparty.entityType == "business"
      } yield counterparty
    }.map(_.entityId)
  }

  def labelsForEntityIDs(graph: LiteGraph, entIDs: Seq[EntityId]): Seq[String] = {
    entIDs.map{entID =>
      val (entityId, entityLabel) = (entID, graph.getNodeById(entID).flatMap(_.label))
      (entityId, entityLabel) match {
        case (id, None) => id.value
        case (_, Some(label)) => label
      }
    }
  }
}
