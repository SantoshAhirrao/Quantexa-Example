package com.quantexa.example.graph.scripting.stages

import java.util.concurrent.TimeUnit

import cats.effect.{IO, Resource}
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.resolver.core.ResolverAPI.{ExpansionRequest, GraphResponse}
import com.quantexa.resource.utils.ResolverClientTimeout
import cats.implicits._
import com.quantexa.example.graph.scripting.GraphScriptTimeoutsConfig
import com.quantexa.graph.script.client.RestClientFactory
import com.quantexa.graph.script.clientrequests.ResolverRestClientExtensions._
import com.quantexa.resolver.proto.rest.client.ResolverProtoRestClient

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object ExpansionStage {

  val stageName = "ExpansionStage"

  def apply(documentIds: Set[DocumentId], expansionRequest: ExpansionRequest, graphScriptTimeoutsConfig: Option[GraphScriptTimeoutsConfig] = None,
            clientFactory: RestClientFactory)
           (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[EntityGraph] = {

    val resolverClient = clientFactory.getResolverResource

    for {
      graphResponse <- buildGraphStep(documentIds, expansionRequest, graphScriptTimeoutsConfig, resolverClient)
    } yield graphResponse.graph
  }

  def buildGraphStep(documentIds: Set[DocumentId], expansionRequest: ExpansionRequest,
                     timeoutsConfig: Option[GraphScriptTimeoutsConfig], resource: Resource[IO, ResolverProtoRestClient])
                    (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[GraphResponse] = {

    implicit val resolverClientTimeout: ResolverClientTimeout = ResolverClientTimeout(
      timeoutsConfig.flatMap(_.resolverResponseTimeout).getOrElse(FiniteDuration(60, TimeUnit.SECONDS)))

      resource.namedMultipleDocumentsExpansion(documentIds,
      expansionRequest,
      stageName,
      timeoutsConfig.flatMap(_.elasticTimeout),
      timeoutsConfig.flatMap(_.internalResolverTimeout))
  }
}
