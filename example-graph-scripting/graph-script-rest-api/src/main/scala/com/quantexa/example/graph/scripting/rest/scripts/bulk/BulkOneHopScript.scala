package com.quantexa.example.graph.scripting.rest.scripts.bulk

import com.quantexa.example.graph.scripting.stages.CustomerOneHopExpansionStage
import com.quantexa.resolver.core.EntityGraph._
import cats.implicits._
import com.quantexa.graph.script.client.RestClientFactory
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics

import scala.concurrent.ExecutionContext

class BulkOneHopScript(clientFactory: RestClientFactory) {

  def apply(documentId: DocumentId)
           (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[EntityGraph] = {

    for {
      oneHopOutput <- CustomerOneHopExpansionStage(documentId, None, clientFactory)
    } yield oneHopOutput
  }
}
