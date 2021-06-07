package com.quantexa.example.graph.scripting.rest

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES

import com.google.common.cache.CacheBuilder
import com.quantexa.example.graph.scripting.rest.api._
import com.quantexa.example.graph.scripting.rest.scripts.bulk.BulkOneHopScript
import com.quantexa.resource.utils.ExecutorConfiguration.Executor
import com.quantexa.resource.utils.DeferredResultUtils.deferredResult
import com.quantexa.security.privilege.Privilege
import com.quantexa.investigation.api.model.InvestigationId
import com.quantexa.resolver.core.EntityGraph._
import javax.servlet.http.HttpServletRequest
import org.audit4j.core.annotation.IgnoreAudit
import org.springframework.web.bind.annotation._
import org.springframework.web.context.request.async.DeferredResult
import org.springframework.beans.factory.annotation.Value
import com.quantexa.resource.annotations.SwaggerDoc
import com.quantexa.example.graph.scripting.rest.scripts.{ExpandScoreAndLoadScript, LinkedRiskyAddressScript, ScoreCustomerOneHopNetworkScript}
import com.quantexa.graph.script.client.RestClientFactory
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.utils.BulkRequesterAPI.{BulkRequesterFail, BulkRequesterResponse, BulkRequesterSuccess, BulkRequesterWaiting}
import com.quantexa.graph.script.utils.EntityGraphWithScore

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.FiniteDuration
import com.quantexa.graph.script.metrics.RestMetricsDashboard._
import com.quantexa.queue.api.Model.WorkId
import com.quantexa.resolver.core.EntityGraphLite.{EntityGraphAsLiteGraph, LiteGraph}
import com.quantexa.rest.client.AuthenticatedRestClient.Credentials

@RestController
@RequestMapping(Array("/graph-scripting"))
class GraphScriptingRestController(
                                   @Value("${quantexa.bulk.cache-size:1000}") cacheSize: Int,
                                   @Value("${quantexa.bulk.cache-timeout:25}") cacheTimeout: Int,
                                   @Value("${quantexa.gateway.uri}") gatewayUri: String,
                                   @Value("${quantexa.client.username}") username: String,
                                   @Value("${quantexa.client.password}") password: String,
                                   @Value("${quantexa.sslEnabled}") sslEnabled: Boolean,
                                   @Value("${quantexa.resolverConfigPath}") resolverConfigPath: String
                                  ) extends Executor {

  private val clientFactory: RestClientFactory = RestClientFactory(gatewayUri, Credentials(username, password), sslEnabled)
  private val expandScoreAndLoadScript = new ExpandScoreAndLoadScript(clientFactory)
  private val scoreCustomerOneHopNetworkScript = new ScoreCustomerOneHopNetworkScript(clientFactory)
  private val linkedRiskyAddress = new LinkedRiskyAddressScript(clientFactory, resolverConfigPath, FiniteDuration(cacheTimeout, TimeUnit.MINUTES))
  private val bulkOneHopScript = new BulkOneHopScript(clientFactory)

  private val cachedRequests = CacheBuilder.newBuilder()
    .maximumSize(cacheSize)
    .expireAfterAccess(cacheTimeout, MINUTES)
    .build[String, FutureErrorOrWithMetrics[EntityGraph]]()

  /* The privilege for each endpoint needs to be added to the example-privilege-dependencies.json
   * and then assigned to the relevant role in config/privilege-mappings.json */
  @SwaggerDoc
  @Privilege(value = "runFullScript")
  @PostMapping(Array("/run-full-script"))
  def runFullScript(
                     @RequestBody request: FullScriptEndpoint,
                     @IgnoreAudit httpRequest: HttpServletRequest): DeferredResult[WorkId] = {

      deferredResult(expandScoreAndLoadScript(request))
    }

  @SwaggerDoc
  @Privilege(value = "scoreCustomerNetwork")
  @PostMapping(Array("/score-customer-network"))
  def scoreCustomerNetwork(
                            @RequestBody request: DocumentId,
                            @IgnoreAudit httpRequest: HttpServletRequest): DeferredResult[EntityGraphWithScore] = {

      deferredResult(scoreCustomerOneHopNetworkScript(request))
    }

  @SwaggerDoc
  @Privilege(value = "linkedRiskyAddress")
  @PostMapping(Array("/linked-risky-address"))
  def linkedRiskyAddress(
                          @RequestBody request: DocumentId,
                          @IgnoreAudit httpRequest: HttpServletRequest): DeferredResult[InvestigationId] = {

      deferredResult(linkedRiskyAddress(request))
    }

  @SwaggerDoc
  @Privilege(value = "oneHopCustomer")
  @PostMapping(Array("/one-hop-customer"))
  def oneHopCustomer(@RequestBody request: DocumentId,
                     @IgnoreAudit httpRequest: HttpServletRequest): DeferredResult[String] =
    deferredResult {
        val recordIds = bulkOneHopScript(request)
        val requestId = s"${UUID.randomUUID.toString}_${request.value}"
        cachedRequests.put(requestId, recordIds)
        Future.successful(requestId)
    }

  /* In order to call the one-hop-customer endpoint in bulk, this check endpoint must be created */
  @SwaggerDoc
  @Privilege(value = "oneHopCustomerCheck")
  @GetMapping(Array("/one-hop-customer-check/{requestId}"))
  def oneHopCustomerCheck(@PathVariable requestId: String): BulkRequesterResponse[LiteGraph] = {
    val response = Option(cachedRequests.getIfPresent(requestId))

    response match {
      case Some(cacheValue) =>
        val result = cacheValue.run.value

        if (result.isCompleted) {
          Await.result(result, new FiniteDuration(200, TimeUnit.MILLISECONDS)) match {
            case Left(errorMessage) =>
              val formattedError = if(errorMessage == null) "null error message found" else errorMessage
              BulkRequesterFail(requestId, formattedError)
            case Right((metrics, output)) =>
              val stageTimes = metrics.groupBy(_.stageName).mapValues(_.map(_.stageTime).sum)
              val liteGraph = EntityGraphAsLiteGraph.convertEntityGraphToLiteGraph(output)
              BulkRequesterSuccess[LiteGraph](requestId,
                liteGraph,
                Some(RestMetrics(Some(createViewablePerformanceMetrics(requestId, stageTimes, metrics)),
                  Some(createViewableLinkingMetrics(requestId, output))))
              )
          }
        } else {
          BulkRequesterWaiting(requestId)
        }
      case None => BulkRequesterFail(requestId, s"$requestId not found in cache.")
    }

  }
}