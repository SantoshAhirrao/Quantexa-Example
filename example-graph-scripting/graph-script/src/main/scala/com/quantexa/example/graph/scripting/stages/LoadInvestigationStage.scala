package com.quantexa.example.graph.scripting.stages

import cats.effect.{IO, Resource}
import com.quantexa.explorer.investigation.proto.rest.client.InvestigationProtoRestClient
import com.quantexa.graph.script.client.RestClientFactory
import com.quantexa.graph.script.clientrequests.{FutureErrorOrWithMetrics, InvestigationRestClientRequest}
import com.quantexa.investigation.api.GraphCustodianProtocol.{InitialiseGraphResponse, InitialisePreresolvedGraph}
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}

import scala.concurrent.ExecutionContext

object LoadInvestigationStage {

  val stageName = "LoadInvestigationStage"

  /**
    * apply method used as this Object is a function (i.e. usage of Object is a singular function call).
    * This stage loads an EntityGraph into the investigation database.
    */
  def apply(documentId: DocumentId, graphToLoad: EntityGraph, clientFactory: RestClientFactory)
                                                              (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[InitialiseGraphResponse] = {

    /* Must import cats implicits to allow a for comprehension computation */
    import cats.implicits._

    val investigationClient = clientFactory.getInvestigationResource

    for {
      investigationResponse <- loadInvestigationStep(documentId, graphToLoad, investigationClient)
    } yield investigationResponse
  }

  /* This has the same function signature as the apply method because this Stage has only 1 Step */
  def loadInvestigationStep(documentId: DocumentId, entityGraph: EntityGraph, resource: Resource[IO, InvestigationProtoRestClient])
                           (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[InitialiseGraphResponse] = {

    val graphToLoad: InitialisePreresolvedGraph = new InitialisePreresolvedGraph(entityGraph, Some(s"From $documentId"), Seq.empty, None, None)
    InvestigationRestClientRequest.loadInvestigation(graphToLoad, stageName, resource)
  }
}
