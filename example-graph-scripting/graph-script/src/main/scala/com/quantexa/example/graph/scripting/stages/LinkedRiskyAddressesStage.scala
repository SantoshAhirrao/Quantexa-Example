package com.quantexa.example.graph.scripting.stages

import cats.effect.{IO, Resource}
import com.quantexa.graph.script.client.{RestClientFactory, ResolverConfigurationForMerge}
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.clientrequests.ResolverRestClientExtensions._
import com.quantexa.graph.script.utils.GraphScriptingUtils.doGraphMergeStep
import com.quantexa.resolver.core.EntityGraph._
import com.quantexa.resolver.core.ResolverAPI._
import com.quantexa.resolver.proto.rest.client.ResolverProtoRestClient

import scala.concurrent.ExecutionContext

/* Naming convention must give business logic. Adding Stage in the name is recommended. */
object LinkedRiskyAddressesStage {

  val stageName = "LinkedRiskyAddressesStage"

  /**
    * apply method used as this Object is a function (i.e. usage of Object is a singular function call).
    * This stage filters the two hop networks with hotlist documents finding
    * all addresses linked to these documents.
    */
  def apply(documentId: DocumentId,
            entityGraphFromPreviousStage: EntityGraph,
            resolverConfig: ResolverConfigurationForMerge,
            clientFactory: RestClientFactory)
           (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[EntityGraph] = {

    /* Must import cats implicits to allow a for comprehension computation */
    import cats.implicits._

    /* This for-comprehension is possible because the return type of generators
     * are FutureErrorOrWithMetrics[T]. */
    val hotlistDocumentIds = entityGraphFromPreviousStage.documents.collect{
      case doc: Document if doc.documentType == "hotlist" => doc.id
    }.toSet

    val resolverClient = clientFactory.getResolverResource

    for {
      riskyAddresses <- expandTwoDegreesAddressOnlyStep(hotlistDocumentIds, resolverClient)
      mergedGraph <- doGraphMergeStep(entityGraphFromPreviousStage, riskyAddresses.graph, resolverConfig)
    } yield mergedGraph

  }

  /* Break Stage down into Steps */
  def expandTwoDegreesAddressOnlyStep(documentIds: Set[DocumentId], resource: Resource[IO, ResolverProtoRestClient])
                                                                  (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[GraphResponse] = {

    import com.quantexa.example.graph.scripting.expansiontemplates.CustomDocumentExpansionTemplate.twoDegreeAddressOnly
    resource.namedMultipleDocumentsExpansion(documentIds, twoDegreeAddressOnly, stageName)
  }
}
