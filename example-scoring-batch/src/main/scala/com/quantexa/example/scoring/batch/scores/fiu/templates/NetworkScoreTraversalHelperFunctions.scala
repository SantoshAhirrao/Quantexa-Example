package com.quantexa.example.scoring.batch.scores.fiu.templates

import com.quantexa.resolver.core.EntityGraph
import com.quantexa.resolver.core.EntityGraph.{EntityId, NodeId}
import com.quantexa.resolver.core.EntityGraphLite._
import com.quantexa.resolver.core.util.{Graph, GraphAlgorithms}
import com.quantexa.resolver.core.util.GraphAlgorithms.PathRestriction

import scala.language.implicitConversions
import scala.languageFeature.{higherKinds, implicitConversions}

object NetworkScoreTraversalHelperFunctions {

  import com.quantexa.analytics.scala.graph.LiteGraphUtils._

  def findPathBetweenNodes(graph: Graph[LiteNode, LiteEdge], start: EntityId, end: EntityId, restrictions: Set[PathRestriction[LiteNode]] = Set.empty): Option[Seq[LiteNode]] = {
    val paths = GraphAlgorithms.findAllSimplePaths[LiteNode,LiteEdge](graph, graph.getLiteNode(start).get, graph.getLiteNode(end).get, restrictions)
    if (paths.nonEmpty) {
      Some(paths.toSeq.minBy(_.length))
    } else None
  }

  def hasPathBetweenNodes(graph: Graph[LiteNode,LiteEdge], start: EntityGraph.EntityId, end: EntityGraph.DocumentId, restrictions: Set[PathRestriction[LiteNode]] = Set.empty): Boolean = {
    GraphAlgorithms.findAllSimplePaths[LiteNode,LiteEdge](graph, graph.getLiteNode(start).get, graph.getLiteNode(end).get, restrictions).nonEmpty
  }
}
