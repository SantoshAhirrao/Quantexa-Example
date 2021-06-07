package com.quantexa.example.scoring.batch.scores.fiu.templates

import com.quantexa.analytics.scala.scoring.model.ScoringModel
import com.quantexa.analytics.scala.scoring.model.ScoringModel.{KeyedBasicScoreOutput, LiteGraphScoreOutput}
import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedLiteGraphScore
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityId}
import com.quantexa.resolver.core.EntityGraphLite.{LiteDocument, LiteEntity, LiteGraph, LiteGraphWithId}
import com.quantexa.scoring.framework.model.ScoreModel

/**
  * Customer Settling To Related Third Parties via address is interpreted as follows:
  * There are relationship documents which are linked by a single address entity (on the counterparty side), where
  * the relationship documents do not link to the same settlement/originating counterparty account (or counterparty business)
  * Social connections are all non-transactional connections.
  */
object CustomerSettlingToRelatedThirdPartiesAddress extends AugmentedLiteGraphScore[LiteGraphScoreOutput[ScoringModel.NetworkKey]] {
//WARNING: This score is not thought to be a good example of how to write this score, a better example will be written in IP-421
  def id = "CustomerSettlingToRelatedThirdPartiesAddress"

  def name: String = "Customer is Settling to Related Third Parties linked via address"

  import NetworkScoreHelperFunctions._
  import com.quantexa.analytics.scala.graph.LiteGraphUtils._

  def score(liteGraph: LiteGraphWithId)(implicit scoreInput: ScoreModel.ScoreInput): Option[LiteGraphScoreOutput[ScoringModel.NetworkKey]] = {
    val (tradesForSubject, counterpartiesToSubject) = identifyTradesAndCounterpartiesForSubject(liteGraph)

    val (subjectNetwork, counterpartyNetworks) = identifySocialNetworkComponents(liteGraph)

    val triggeredGraphs = getSubgraphTradesLinkedViaCommonAddress(counterpartyNetworks, tradesForSubject).toSet

    val triggeredNetworksCount = triggeredGraphs.size

    val underlyingScores = generateUnderlyingScores(tradesForSubject, triggeredGraphs)

    if (triggeredNetworksCount > 0) {
      Some(new LiteGraphScoreOutput(
        keys = ScoringModel.NetworkKey(liteGraph.graphSubjectId.get),
        severity = Some(100),
        band = Some("HIGH"),
        description = Some(s"The subject of the graph is trading with linked third parties: ${describeLinkedCounterparties(triggeredGraphs.map(_._2))}"),
        underlyingScores = underlyingScores))
    } else None
  }

  private def getSubgraphTradesLinkedViaCommonAddress(counterPartyGraphs: Seq[LiteGraph],
                                                      customerTrades: Set[DocumentId]): Seq[(LiteGraph, Seq[String])] = {
    import com.quantexa.analytics.scala.graph.LiteGraphUtils._
    case class LiteGraphTradesWithAddress(cpLiteGraph: LiteGraph, addressInLg: LiteEntity, customerCounterpartyRelationshipDoc: Seq[LiteDocument])

    val customerTradesLinkedToAddress = {
      for {
        cpGraph <- counterPartyGraphs
        address <- cpGraph.getEntitiesByType("address")
      } yield LiteGraphTradesWithAddress(cpGraph,
        address,
        cpGraph.getConnectedNodes(address.entityId).filter(doc => doc.documentType == "trade" && customerTrades(doc.documentId))
      )
    }.filter(_.customerCounterpartyRelationshipDoc.length >= 2)

    val triggeredGraphs = customerTradesLinkedToAddress.
      filter { case LiteGraphTradesWithAddress(graph, _, relationshipDocs) =>

        val relationshipDocIDs = relationshipDocs.map(_.documentId).toSet
        val counterPartyEntityIDs = (for {
          relDoc <- relationshipDocs
          entsOnRelationshipDocs <- graph.getConnectedNodes(relDoc.documentId)
          if Set("account", "business")(entsOnRelationshipDocs.entityType)
        } yield entsOnRelationshipDocs.entityId).toSet

        val keepRelationshipDocsAndCpEntities = graph.
          deleteEntitiesById(graph.entities.map(_.entityId).filterNot(counterPartyEntityIDs(_)).toSet).
          deleteDocumentsById(graph.docs.map(_.documentId).filterNot(relationshipDocIDs(_)).toSet)
        val connectedTradeSubNetworks = keepRelationshipDocsAndCpEntities.splitGraph
        // if all trades appear on the same subgraph then they are linked via business or account - therefore not interesting for this score
        connectedTradeSubNetworks.length > 1
      }
    triggeredGraphs.map(res => (res.cpLiteGraph, labelsForEntityIDs(res.cpLiteGraph, Seq(res.addressInLg.entityId))))
  }

  private def describeLinkedCounterparties(graphs: Set[Seq[String]]): String = {
    graphs.map(graphCPs => s"Linked set - ${graphCPs.mkString(", ")}.").mkString("").trim
  }

  private def generateUnderlyingScores(tradesForSubject: Set[DocumentId], triggeredGraphs: Set[(LiteGraph, Seq[String])]): Seq[KeyedBasicScoreOutput] = {
    for {
      graph <- triggeredGraphs.toSeq
      trade <- graph._1.getDocumentsByType("trade") if tradesForSubject(trade.documentId)
    } yield underlyingScoreForTrade(trade, graph._2)
  }

  private def underlyingScoreForTrade(trade: LiteDocument, entities: Seq[String]): KeyedBasicScoreOutput = {
    val description = s"Counterparty of trade ${trade.documentId.value} is related to another counterparty of the graph subject. ${entities.mkString(", ")}."
    new KeyedBasicScoreOutput(keyValues = Seq(ScoringModel.LookupKey(name = "relationshipKey", stringKey = Some(trade.documentId.value), dateKey = None, intKey = None, longKey = None)),
      docType = "trade",
      severity = Some(100),
      band = Some("HIGH"),
      description = Some(description))
  }
}