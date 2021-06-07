package com.quantexa.example.graph.scripting.stages

import cats.effect.{IO, Resource}
import com.fasterxml.jackson.databind.JsonNode
import com.quantexa.document.json.rest.client.DocumentJsonRestClient
import com.quantexa.graph.script.client._
import com.quantexa.graph.script.clientrequests.{DocumentRestClientRequest, FutureErrorOrWithMetrics, ScoringRestClientRequest}
import com.quantexa.graph.script.utils.EntityGraphWithScore
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.scoring.api.ScoringAPI.ScoreResponse
import com.quantexa.scoring.proto.rest.client.ScoringProtoRestClient

import scala.concurrent.ExecutionContext

/* Naming convention must give business logic. Adding Stage in the name is recommended. */
object ScoreCustomerOneHopNetworkStage {

  val stageName = "ScoreCustomerOneHopNetworkStage"

  /**
    * apply method used as this Object is a function (i.e. usage of Object is a singular function call).
    * This stage requests all the documents for an EntityGraph and scores the graph and documents.
    * Returns both the EntityGraph and associated scores.
    */
  def apply(entityGraph: EntityGraph, clientFactory: RestClientFactory)
           (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[EntityGraphWithScore] = {

    val documentClient = clientFactory.getDocumentResource
    val scoringClient = clientFactory.getScoringResource

    /* Must import cats implicits to allow a for comprehension computation */
    import cats.implicits._
    /* This for-comprehension is possible because the return type of generators
     * are FutureErrorOrWithMetrics[T]. */
    for {
      graphWithDocumentSources <- getDocumentStep(entityGraph, documentClient)
      graphWithScoredOutput <- scoreGraphStep(graphWithDocumentSources, "fiu-smoke", scoringClient)
    } yield EntityGraphWithScore(entityGraph, graphWithScoredOutput.outputs)
  }

  /* Break Stage down into Steps */
  def getDocumentStep(entityGraph: EntityGraph, resource: Resource[IO, DocumentJsonRestClient])
  (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[(EntityGraph, Map[DocumentId, JsonNode])] = {

    DocumentRestClientRequest.getDocumentSources(entityGraph, stageName, resource)
  }

  /* Break Stage down into Steps */
  def scoreGraphStep(graphWithDocumentSources: (EntityGraph, Map[DocumentId, JsonNode]),
  scoreSet: String, resource: Resource[IO, ScoringProtoRestClient])
  (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[ScoreResponse] = {

    ScoringRestClientRequest.addScoresToGraph(graphWithDocumentSources, scoreSet, stageName, resource)
  }
}
