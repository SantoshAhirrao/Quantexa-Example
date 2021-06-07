package com.quantexa.example.scoring.batch.scores.fiu.templates

import com.quantexa.resolver.core.EntityGraphLite.{LiteDocument, LiteEntity, LiteGraph, LiteGraphWithId}
import com.quantexa.analytics.scala.scoring.model.ScoringModel.{KeyedBasicScoreOutput, LiteGraphScoreOutput}
import com.quantexa.analytics.scala.scoring.model.ScoringModel
import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedLiteGraphScore
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityId}
import com.quantexa.scoring.framework.model.ScoreModel

/**
  * Customer Settling To Related Third Parties via Business is interpreted as follows:
  * There are settlements to businesses (where these businesses are not the customer themselves) where those businesses are socially connected.
  * Social connections are all non-transactional connections.
  * WARNING: This score is not thought to be a good example of how to write this score, but does illustrate one way of writing network scores. A better example is CustomerSettlingToRelatedThirdPartiesBusinessV2
  */
object CustomerSettlingToRelatedThirdPartiesBusiness extends AugmentedLiteGraphScore[LiteGraphScoreOutput[ScoringModel.NetworkKey]] {

  def id = "CustomerSettlingToRelatedThirdPartiesBusiness"

  def name: String = "Customer is Settling to Related Third Party business entities"

  import com.quantexa.analytics.scala.graph.LiteGraphUtils._
  import NetworkScoreHelperFunctions._

  def score(liteGraph: LiteGraphWithId)(implicit scoreInput: ScoreModel.ScoreInput): Option[LiteGraphScoreOutput[ScoringModel.NetworkKey]] = {
    val (tradesForSubject, counterpartiesToSubject) = identifyTradesAndCounterpartiesForSubject(liteGraph)

    val (subjectNetwork, counterpartyNetworks) = identifySocialNetworkComponents(liteGraph)

    val triggeredGraphs = getSubgraphMultipleCounterpartiesToSubject(counterpartyNetworks, counterpartiesToSubject).toSet

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

  private def getSubgraphMultipleCounterpartiesToSubject(graphs: Seq[LiteGraph],
  counterPartyBusinesses: Set[EntityId]): Seq[(LiteGraph, Seq[String])] = {
    val triggeredGraphs = graphs.map { graph =>
      (graph, graph.entities.filter(entity => counterPartyBusinesses(entity.entityId)).map(_.entityId))
    }.filter{case (_, entities) => entities.length >= 2}
    triggeredGraphs.map{case (lg, entities) => (lg, labelsForEntityIDs(lg, entities))}
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