package com.quantexa.example.graph.scripting.rest.scripts

import com.quantexa.example.graph.scripting.stages.{CustomerOneHopExpansionStage, ScoreCustomerOneHopNetworkStage}
import com.quantexa.graph.script.client.RestClientFactory
import com.quantexa.graph.script.utils.EntityGraphWithScore
import com.quantexa.resolver.core.EntityGraph.DocumentId

import scala.concurrent.{ExecutionContext, Future}

class ScoreCustomerOneHopNetworkScript(clientFactory: RestClientFactory) {

  def apply(documentId: DocumentId)
           (implicit ec: ExecutionContext): Future[EntityGraphWithScore] = {

    /* Must import cats implicits to allow a for comprehension computation */
    import cats.implicits._

    val result = for {
      oneHopOutput <- CustomerOneHopExpansionStage(documentId, None, clientFactory)
      entityGraphWithScore <- ScoreCustomerOneHopNetworkStage(oneHopOutput, clientFactory)
    } yield entityGraphWithScore

    result.value.value.flatMap {
      case Right(entityGraphWithScore) => Future.successful(entityGraphWithScore)
      case Left(msg) => Future.failed(throw new IllegalStateException(msg))
    }
  }
}
