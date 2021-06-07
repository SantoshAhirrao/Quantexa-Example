package com.quantexa.example.graph.scripting.rest.scripts

import java.util.concurrent.TimeUnit.SECONDS

import com.quantexa.graph.script.client.RestClientFactory
import com.quantexa.graph.script.utils.BatchResolverConfigUtils._
import scala.concurrent.duration.FiniteDuration
import akka.util.Timeout
import com.quantexa.example.graph.scripting.stages.{CustomerTwoHopIndividualsOnlyStage, LinkedRiskyAddressesStage, LoadInvestigationStage}
import com.quantexa.graph.script.client.ResolverConfigurationForMerge
import com.quantexa.investigation.api.model.InvestigationId
import com.quantexa.resolver.core.EntityGraph.DocumentId
import scalacache._, scalacache.guava._, scalacache.modes.scalaFuture._
import cats.implicits._
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.clientrequests.metrics.LocalStepWithMetrics

import scala.concurrent.{ExecutionContext, Future}

class LinkedRiskyAddressScript(clientFactory: RestClientFactory,
                               resolverConfigurationForMergePath: String,
                               cacheTimeoutMinutes: FiniteDuration) {

  implicit val timeout: Timeout = Timeout(5, SECONDS)
  implicit val guavaCache: Cache[ResolverConfigurationForMerge] = GuavaCache[ResolverConfigurationForMerge]

  // Cache resolverConfig so that Quanfiguration doesn't need to be queried each time the endpoint is called.
  private def getOrUpdateConfigInCache()(implicit ec: ExecutionContext): FutureErrorOrWithMetrics[ResolverConfigurationForMerge] = {
    LocalStepWithMetrics(
      cachingF("resolverConfig")(Some(cacheTimeoutMinutes)) {
        Future.successful(resolverConfigGraphScripting(resolverConfigurationForMergePath))
      }, "GetResolverConfig"
    )
  }

  def apply(docId: DocumentId)
           (implicit ec: ExecutionContext): Future[InvestigationId] = {

    val result = for {
      resolverConfigForMerge <- getOrUpdateConfigInCache()
      twoHopNetwork <- CustomerTwoHopIndividualsOnlyStage(docId, clientFactory)
      riskyAddressGraph <- LinkedRiskyAddressesStage.apply(docId, twoHopNetwork, resolverConfigForMerge, clientFactory)
      res <- LoadInvestigationStage.apply(docId, riskyAddressGraph, clientFactory)
    } yield res

    result.value.value.flatMap(_ match {
      case Right(graphResponse) => Future.successful(graphResponse.id)
      case Left(msg) => Future.failed(throw new IllegalStateException(msg))
    })
  }
}