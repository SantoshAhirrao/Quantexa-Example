package com.quantexa.example.graph.scripting.stages

import cats.effect.{IO, Resource}
import com.quantexa.example.graph.scripting.GraphScriptingConfig
import com.quantexa.graph.script.client.RestClientFactory
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.clientrequests.ResolverRestClientExtensions._
import com.quantexa.graph.script.expansiontemplates.StandardDocumentExpansionTemplate
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.resolver.core.ResolverAPI.GraphResponse
import com.quantexa.resolver.proto.rest.client.ResolverProtoRestClient

import scala.concurrent.ExecutionContext

/* The object name should encapsulate business logic, and should always be appended with Stage.
 * Each stage should be an object with an 'apply' method with steps that should be defined
 * separately as a function, even if there is only one step. */
object CustomerOneHopExpansionStage {

  val stageName = "CustomerOneHopExpansionStage"

  /* Clients and contexts should form a separate parameter list and implicit where possible. */
  /**
    * This stage does a simple one hop expansion from the document.
    */
  def apply(documentId: DocumentId, config: Option[GraphScriptingConfig] = None, clientFactory: RestClientFactory)
           (implicit ec: ExecutionContext)
              : FutureErrorOrWithMetrics[EntityGraph] = {

    /* Must import cats implicits to allow a for comprehension computation */
    import cats.implicits._

    val resolverClient = clientFactory.getResolverResource

    /*Create a for-comprehension which has one line per step in the stage. Each line should reference a function, defined
    * below, that performs the step logic.
     */
    for {
      graphResponse <- buildGraphStep(documentId, config, resolverClient)
    } yield graphResponse.graph
  }

  /* Each step function name should be appended with Step. This is where the business logic of the step is performed.
  * You should have one function per step. */
  def buildGraphStep(documentId: DocumentId, config: Option[GraphScriptingConfig] = None, resource: Resource[IO, ResolverProtoRestClient])
                    (implicit ec: ExecutionContext)
                      : FutureErrorOrWithMetrics[GraphResponse] = {

    resource.namedMultipleDocumentsExpansion(Set(documentId),
      StandardDocumentExpansionTemplate.defaultDocumentExpansion,
      stageName,
      config.flatMap(_.timeouts.elasticTimeout),
      config.flatMap(_.timeouts.internalResolverTimeout))
  }
}
