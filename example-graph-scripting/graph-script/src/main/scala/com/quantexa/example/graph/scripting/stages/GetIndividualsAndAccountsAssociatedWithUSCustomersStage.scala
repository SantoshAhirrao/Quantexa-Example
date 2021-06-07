package com.quantexa.example.graph.scripting.stages

import cats.effect.{IO, Resource}
import cats.implicits._
import com.quantexa.graph.script.client.{RestClientFactory, ResolverConfigurationForMerge}
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.clientrequests.ResolverRestClientExtensions._
import com.quantexa.graph.script.utils.GraphScriptingUtils.doGraphMergeStep
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.resolver.core.ResolverAPI.GraphResponse
import com.quantexa.resolver.core.configuration.Definitions.ResolutionTemplateName
import com.quantexa.resolver.proto.rest.client.ResolverProtoRestClient

import scala.concurrent.ExecutionContext

object GetIndividualsAndAccountsAssociatedWithUSCustomersStage {

  val stageName = "GetIndividualsAndAccountsAssociatedWithUSCustomersStage"

  def apply(key: DocumentId,
            entityGraph: EntityGraph,
            resolverConfig: ResolverConfigurationForMerge,
            clientFactory: RestClientFactory)
           (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[EntityGraph] = {

    val resolverClient = clientFactory.getResolverResource

    for {
      entitiesToAdd <- addNewRecordsToGraphStep(key, entityGraph, resolverClient)
      mergedGraph <- doGraphMergeStep(entityGraph, entitiesToAdd.graph, resolverConfig)
    } yield mergedGraph
  }

  /*
  The aim of this step is to find all individual and account entities linked to documents in the graph, and add them to
  the graph without re-resolving anything already on the graph. Note that we do not need to create the graph Edges, as
  they are created by the graph merge.

  First, find all the records linked to the graph that are of either "individual" or "account" type. This is done by
  looking at all of the records on all of the documents on the EntityGraph. Next, we find all of the records that
  correspond to entities already on the graph, by taking the entities on the EntityGraph and looking at the records that
  make up that entity. We then take a set difference so that we have only the records corresponding to entities NOT already
  on the EntityGraph, and lastly we map the records to the form Set[Set[Record]], since this is what's required for the
  resolver request.
  NOTE: Each inner set should contain all records known to be part of the same entity. If you do not know which entity
  a record corresponds to, it should go in a set on its own. E.g. here each inner set has size 1.

  Lastly, we make a resolver request for these records. Again note that we resolve ONLY entities that have not already
  been resolved, and then merge them in to the original EntityGraph (i.e. add them on). Nothing in the final entity graph
  has been resolved more than once (throughout the stages), meaning our graph script has not duplicated work.
   */
  def addNewRecordsToGraphStep(key: DocumentId,
                               entityGraph: EntityGraph,
                               resource: Resource[IO, ResolverProtoRestClient])
                              (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[GraphResponse] = {

    val recordsOnGraph = entityGraph.documents.flatMap(_.records).filter(record =>
      record.entityType == "individual" || record.entityType == "account"
    ).toSet
    val recordsAlreadyOnEntityGraph = entityGraph.entities.flatMap(_.records.values).flatten.map(_.record).toSet
    val recordsToResolve = (recordsOnGraph -- recordsAlreadyOnEntityGraph).map(Set(_))


    val resolutionTemplate = Map(
      "individual" -> new ResolutionTemplateName("default"),
      "account" -> new ResolutionTemplateName("default")
    )

    resource.buildEntities(recordsToResolve, resolutionTemplate, stageName)
  }
}
