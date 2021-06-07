package com.quantexa.example.graph.scripting.stages

import cats.effect.{IO, Resource}
import com.quantexa.graph.script.client.RestClientFactory
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.clientrequests.ResolverRestClientExtensions._
import com.quantexa.resolver.core.EntityGraph._
import com.quantexa.resolver.core.ResolverAPI._
import com.quantexa.resolver.proto.rest.client.ResolverProtoRestClient

import scala.concurrent.ExecutionContext

/* Naming convention must give business logic. Adding Stage in the name is recommended. */
object CustomerTwoHopIndividualsOnlyStage {

  val stageName = "CustomerTwoHopIndividualsOnlyStage"

  /**
    * apply method used as this Object is a function (i.e. usage of Object is a singular function call).
    * This stage expands from the original document through individuals
    */
  def apply(documentId: DocumentId, clientFactory: RestClientFactory)
            (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[EntityGraph] = {
    /* Must import cats implicits to allow a for comprehension computation */
    import cats.implicits._

    val resolverClient = clientFactory.getResolverResource

    /* This for-comprehension is possible because the return type of generators
     * are FutureErrorOrWithMetrics[T]. */
    for {
      graphResponse <- expandTwoDegreesIndividualsOnlyStep(documentId)(resolverClient)
    } yield graphResponse.graph

  }

  /* Break Stage down into Steps */
  def expandTwoDegreesIndividualsOnlyStep(documentId: DocumentId)(resource: Resource[IO, ResolverProtoRestClient])
                                                                  (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[GraphResponse] = {

    import com.quantexa.example.graph.scripting.expansiontemplates.CustomDocumentExpansionTemplate.twoDegreeIndividualsOnly
    resource.namedMultipleDocumentsExpansion(Set(documentId), twoDegreeIndividualsOnly, stageName)
  }
}
