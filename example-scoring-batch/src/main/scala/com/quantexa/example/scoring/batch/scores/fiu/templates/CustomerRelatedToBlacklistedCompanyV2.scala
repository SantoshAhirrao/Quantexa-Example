package com.quantexa.example.scoring.batch.scores.fiu.templates

import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.resolver.core.EntityGraphLite._
import com.quantexa.analytics.scala.scoring.model.ScoringModel.{LiteGraphScoreOutput, _}
import com.quantexa.resolver.core.util.GraphAlgorithms.PathRestriction
import com.quantexa.resolver.core.util.{Graph, GraphAlgorithms}
import com.quantexa.scoring.framework.model.scores.LiteGraphScore
import scala.language.implicitConversions
import scala.languageFeature.{higherKinds, implicitConversions}

/**
  * Customer Related to Blacklisted Company is interpreted as follows:
  * The customer's social network is defined as all entities who are connected to the customer via non trade/transactional links.
  * If there is a hotlisted business who is in the customer's social network (and is not the customer themselves) then the rule fires.
  *
  * [[CustomerRelatedToBlacklistedCompanyV1]] is the preferred version of this rule, however this example demonstrates using path restrictions
  *
  */
object CustomerRelatedToBlacklistedCompanyV2 extends LiteGraphScore[LiteGraphScoreOutput[NetworkKey]] {

  val id: String = "CustomerRelatedToBlacklistedCompany"

  def score(liteGraphWithId: LiteGraphWithId)(implicit scoreInput: ScoreModel.ScoreInput): Option[LiteGraphScoreOutput[NetworkKey]] = {

    import com.quantexa.analytics.scala.graph.LiteGraphUtils._

    val liteGraph = liteGraphWithId.graph
    val graph: Graph[LiteNode, LiteEdge] = liteGraph
    //TODO: IP-509 Replace with logger.error.
    val graphSubjectId = liteGraphWithId.graphSubjectId.getOrElse(
      throw new IllegalStateException("Graph built with graph scripting must have subject id.")
    )

    val customerNode = graph.getLiteNode(graphSubjectId).
      getOrElse(throw new IllegalStateException(s"Can't find subject $graphSubjectId of graph"))

    val hotlistNodes = graph.getLiteNodeByDocType("hotlist")

    def notContainsTransactionOrTrade(nodes: Seq[LiteNode]): Boolean = !nodes.exists(n => Set("transaction", "trade")(n.documentType.getOrElse("")))

    val doesNotContainTxnOrTrade = PathRestriction(activationLength = 1, restriction = notContainsTransactionOrTrade)

    val socialConnectionPathRestrictions = Set(doesNotContainTxnOrTrade)

    val pathsFromHotlistNodeToCustomerDoc = hotlistNodes.map(hotlist =>
      hotlist -> GraphAlgorithms.findAllSimplePaths(liteGraph, customerNode, hotlist, socialConnectionPathRestrictions))

    val removeDirectlyConnectedHotLists = pathsFromHotlistNodeToCustomerDoc.filter {
      case (_, allPaths) =>
        !allPaths.exists { x => x.size == 3 } //Remove directly connected
    }

    val hotlistsOfInterest = removeDirectlyConnectedHotLists.flatMap(_._1.toLiteDoc)

    val hotlistedEntities = for {
      hotlist <- hotlistsOfInterest
      hotlistedEntity <- liteGraph.getConnectedNodes(hotlist)
      if hotlistedEntity.entityType == "business"
    } yield hotlistedEntity

    val hotlistedEntityLabels = hotlistedEntities.map(e => e.label)

    if (hotlistedEntityLabels.nonEmpty) Some(LiteGraphScoreOutput(
      keys = NetworkKey(graphSubjectId),
      severity = Some(100),
      band = None,
      description = Some(s"""The following customers are blacklisted and connected to focal customer: ${hotlistedEntityLabels.mkString(",")}"""),
      underlyingScores = Seq.empty
    )) else None
  }
}
