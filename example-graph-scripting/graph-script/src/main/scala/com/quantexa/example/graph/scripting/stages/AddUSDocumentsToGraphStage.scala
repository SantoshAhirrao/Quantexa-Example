package com.quantexa.example.graph.scripting.stages

import cats.effect.{IO, Resource}
import com.quantexa.document.api.DocumentAPI.DocumentsRequest
import com.quantexa.document.json.rest.client.DocumentJsonRestClient
import com.quantexa.example.graph.scripting.GraphScriptingConfig
import com.quantexa.graph.script.client.{ResolverConfigurationForMerge, RestClientFactory}
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.clientrequests.ResolverRestClientExtensions._
import com.quantexa.graph.script.clientrequests.metrics.DocumentsResponseWithMetrics
import com.quantexa.graph.script.utils.GraphScriptingUtils.doGraphMergeStep
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.resolver.core.ResolverAPI.GraphResponse
import com.quantexa.resolver.proto.rest.client.ResolverProtoRestClient

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

object AddUSDocumentsToGraphStage {

  val stageName = "AddUSDocumentsToGraphStage"

  def apply(key: DocumentId,
            inputEntityGraph: EntityGraph,
            resolverConfig: ResolverConfigurationForMerge,
            config: Option[GraphScriptingConfig] = None,
            clientFactory: RestClientFactory)(implicit ec: ExecutionContext): FutureErrorOrWithMetrics[EntityGraph] = {

    import cats.implicits._

    val resolverClient = clientFactory.getResolverResource
    val documentClient = clientFactory.getDocumentResource

    for {
      usCustomers <- getUSCustomerDocumentsStep(key, inputEntityGraph, documentClient)
      documentGraph <- buildDocumentsStep(usCustomers.toSet, config, resolverClient)
      mergedGraph <- doGraphMergeStep(inputEntityGraph, documentGraph.graph, resolverConfig)
    } yield mergedGraph

  }

  /*
  Find all of the documents linked to the input graph (but not on the graph) by looking at the records
  associated with the individual entities on the input graph. Then use the document client to get the document source
  for each of these documents and perform some logic to filter them (here we want only US customer documents).
   */
  def getUSCustomerDocumentsStep(key: DocumentId,
                                 entityGraph: EntityGraph,
                                 documentClient: Resource[IO, DocumentJsonRestClient]): FutureErrorOrWithMetrics[Seq[DocumentId]] = {

    val documentsLinkedToIndividualsOnGraph = entityGraph.entities.flatMap(_.records.keys).toSet -- Set(key)

    DocumentsResponseWithMetrics(
      documentClient.use { c =>
        c.documentRequest(DocumentsRequest(documentsLinkedToIndividualsOnGraph))
        .map(docResp => docResp.documents
          .filter(document =>
            (document.documentType == "customer") && document.source.path("nationality").asText() == "US")
          .map(_.documentId)
        )
      }.unsafeToFuture(), stageName)
  }

  /*
  Perform a resolver request for the document ids found in the previous step. Performing the request like this means that
  only the documents that were not already on the graph are resolved. I.e. nothing is resolved twice, meaning it is
  efficient. The resulting graph of documents is then merged with the original entity graph, in effect adding the documents
  to the original entity graph.
   */
  def buildDocumentsStep(documentIds: Set[DocumentId],
                         config: Option[GraphScriptingConfig] = None,
                         resolverClient: Resource[IO, ResolverProtoRestClient]): FutureErrorOrWithMetrics[GraphResponse] = {

    resolverClient.buildDocuments(documentIds,
      stageName,
      config.flatMap(_.timeouts.elasticTimeout),
      config.flatMap(_.timeouts.internalResolverTimeout))
  }
}
