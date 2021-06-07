package com.quantexa.example.taskloading.utils

import com.quantexa.resolver.core.EntityGraph.{DocumentId, Record, _}
import com.quantexa.resolver.core.EntityGraphLite.{LiteGraph, UntypedRecord}
import com.quantexa.resolver.core.ResolverAPI._
import com.quantexa.resolver.core.configuration.Definitions.ResolutionTemplateName
import com.quantexa.resolver.ingest.Model.id

object ResolverUtils {

  val defaultResolutionTemplate: Map[String, ResolutionTemplateName] = Map(
    "individual" -> new ResolutionTemplateName("default"),
    "business" -> new ResolutionTemplateName("default"),
    "account" -> new ResolutionTemplateName("default"),
    "address" -> new ResolutionTemplateName("default"),
    "telephone" -> new ResolutionTemplateName("default"))

  /**
    * Convert from LiteGraph record to EntityGraph record
    */
  def untypedRecordToRecord(r: UntypedRecord): Record = {
    new Record(
      id = new RecordId(r.recordId),
      entityType = new EntityType(r.entityType),
      documentType = r.documentType
    )
  }

  /**
    * Build a resolver request that can be used to create an EntityGraph, given a LiteGraph
    */
  def createResolverRequest(customerDocId: DocumentId, graph: Option[LiteGraph]): ResolverRequest = {
    graph match {
      case Some(builtGraph) => {
        val documentsRequest = new DocumentsRequest(builtGraph.docs.map(_.documentId).toSet)

        val untypedRecordSets = builtGraph.entities.map(_.records).toSet
        val records = untypedRecordSets.map(urs => urs.map(ur => untypedRecordToRecord(ur)))
        val recordSets = records.map(_.toSet)

        val entitiesRequest = new EntitiesRequest(defaultResolutionTemplate, recordSets)
        new ResolverRequest(Some(Seq(entitiesRequest)), Some(Seq(documentsRequest)), None, None, false, None, None)
      }
      case None => {
        val customerDocumentRequest = new DocumentsRequest(Set(customerDocId))
        new ResolverRequest(None, Some(Seq(customerDocumentRequest)), None, None, false, None, None)
      }
    }
  }

}
