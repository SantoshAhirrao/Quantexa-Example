package com.quantexa.example.scoring.batch.scores.fiu.templates

import scala.languageFeature.{higherKinds, implicitConversions}
import scala.language.implicitConversions
import com.quantexa.resolver.core.EntityGraphLite._
import com.quantexa.resolver.core.util.Graph
import com.quantexa.resolver.core.util.GraphAlgorithms.PathRestriction
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.analytics.scala.graph.LiteGraphTraversals._
import com.quantexa.analytics.scala.graph.LiteGraphUtils._
import com.quantexa.analytics.scala.scoring.model.ScoringModel.LiteGraphScoreOutput
import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedLiteGraphScore
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityId}

/**
  * Customer Settling To Related Third Parties via Business is interpreted as follows:
  * There are settlements to businesses (where these businesses are not the customer themselves) where those businesses are socially connected.
  * Social connections are all non-transactional connections.
  */
object CustomerSettlingToRelatedThirdPartiesBusinessV2 extends AugmentedLiteGraphScore[LiteGraphScoreOutput[NetworkKey]] {

  def id = "CustomerSettlingToRelatedThirdPartiesBusinessV2"

  def name: String = "Customer is Settling to Related Third Party business entities - Method 2"

  import NetworkScoreTraversalHelperFunctions._

  val tradesNotOnPath: Seq[LiteNode] => Boolean = nodes => !nodes.exists(_.documentType.contains("trade"))
  val tradesNotOnPathRestriction = PathRestriction(activationLength = 1, restriction = tradesNotOnPath)

  def score(liteGraph: LiteGraphWithId)(implicit scoreInput: ScoreModel.ScoreInput): Option[LiteGraphScoreOutput[NetworkKey]] = {

    val graph: Graph[LiteNode, LiteEdge] = liteGraph.graph //Implicit conversion is provided from the import for com.quantexa.analytics.scala.graph.LiteGraphUtils._

    val subjectBusinesses = Traverser.documents(_.documentId == liteGraph.graphSubjectId.get)
      .entities(predicateEntity = e => e.entityType == "business")

    val counterpartiesToSubject = subjectBusinesses.
      documents(predicateDocument = d => d.documentType == "trade").
      entities(
        predicateEntity = ent => {
          val subjectEntityIDs = subjectBusinesses.run(liteGraph.graph).map(_.entityId).toSet
          ent.entityType == "business" && !subjectEntityIDs(ent.entityId)
        }
      )

    val counterpartiesNoLinkToSubjectTraverser = for {
      counterpartyEntity <- counterpartiesToSubject if !sociallyLinkedNodes(graph, counterpartyEntity, liteGraph.graphSubjectId.get)
    } yield counterpartyEntity

    //At a later point, we could use this path to facilitate showing it in the UI?
    case class PathsBetweenCounterparties(cp1:EntityId, cp2:EntityId, path:Seq[LiteNode])

    val counterpartiesWithPaths = for {
      counterparty1 +: counterparty2 +: Nil <- counterpartiesNoLinkToSubjectTraverser.run(liteGraph.graph).combinations(2)
      path <- socialPathBetween(graph, counterparty1, counterparty2)
    } yield PathsBetweenCounterparties(counterparty1.entityId, counterparty2.entityId, path)

    if (counterpartiesWithPaths.nonEmpty) {
      val triggeredPaths = counterpartiesWithPaths.toSeq
      val linkedCounterpartyDescription = describeLinkedCounterparties(triggeredPaths.map{ x=> (x.cp1,x.cp2)}, liteGraph)

      Some(LiteGraphScoreOutput(
        keys = NetworkKey(liteGraph.graphSubjectId.get),
        severity = Some(100),
        band = Some("HIGH"),
        description = Some(s"The subject of the graph is trading with linked third parties: $linkedCounterpartyDescription"),
        underlyingScores = Seq.empty))
    } else None
  }

  private def socialPathBetween(graph: Graph[LiteNode, LiteEdge], counterparty1: LiteEntity, counterparty2: LiteEntity): Option[Seq[LiteNode]] = {
    findPathBetweenNodes(graph, counterparty1.entityId, counterparty2.entityId, Set(tradesNotOnPathRestriction))
  }

  private def sociallyLinkedNodes(graph: Graph[LiteNode, LiteEdge], counterpartyEntity: LiteEntity, subject: DocumentId): Boolean = {
    hasPathBetweenNodes(graph, counterpartyEntity.entityId, subject, Set(tradesNotOnPathRestriction))
  }

  private def describeLinkedCounterparties(linkedCounterpartyPairs: Seq[(EntityId, EntityId)], lg: LiteGraphWithId): String = {
    val descSeq = linkedCounterpartyPairs.map{ case (e1, e2) =>
      val e1Label = lg.graph.entities.find(_.entityId == e1).flatMap(_.label).getOrElse(e1.value)
      val e2Label = lg.graph.entities.find(_.entityId == e2).flatMap(_.label).getOrElse(e2.value)
      s"Linked set - $e1Label, $e2Label."
    }

    descSeq.mkString(" ")
  }
}