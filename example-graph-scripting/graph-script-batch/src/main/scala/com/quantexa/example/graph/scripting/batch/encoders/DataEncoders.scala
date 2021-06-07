package com.quantexa.example.graph.scripting.batch.encoders

import cats.effect.IO
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import org.apache.spark.sql.{Encoder, Encoders}
import com.quantexa.investigation.api.GraphCustodianProtocol.InitialiseGraphResponse
import com.quantexa.explorer.tasks.api.TaskAPI
import com.quantexa.graph.script.client.ClientResponseWrapper
import com.quantexa.graph.script.utils.BulkRequesterAPI.{BulkRequesterResponse, BulkRequesterResponseWithTiming}
import com.quantexa.graph.script.utils.EntityGraphWithScore
import com.quantexa.queue.api.Model.WorkId
import com.quantexa.resolver.core.EntityGraphLite.LiteGraph

object DataEncoders {

  /* There are the kryo encoders required by the various batch graph scripts. Whenever a dataset of non-primitive data
   * types is output, we need an implicit kryo encoder in scope. For example, if our script creates a Dataset[EntityGraph],
   * it will also need to import entityGraphEncoder. Place all encoders required by your scripts here.
   */

  implicit val entityGraphEncoder: Encoder[EntityGraph] = Encoders.kryo[EntityGraph]
  implicit val entityGraphWithScoreEncoder: Encoder[EntityGraphWithScore] = Encoders.kryo[EntityGraphWithScore]
  implicit val clientResponseEntityGraphEncoder: Encoder[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]]
    = Encoders.kryo[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]]
  implicit val clientResponseEntityGraphEntityGraphEncoder: Encoder[ClientResponseWrapper[DocumentId, EntityGraph, EntityGraph]]
  = Encoders.kryo[ClientResponseWrapper[DocumentId, EntityGraph, EntityGraph]]
  implicit val documentIdWithEntityGraphEncoder: Encoder[(DocumentId, EntityGraph)] = Encoders.tuple(Encoders.kryo[DocumentId], entityGraphEncoder)
  implicit val documentIdWithEntityGraphWithScoreEncoder: Encoder[(DocumentId, EntityGraphWithScore)] = Encoders.tuple(Encoders.kryo[DocumentId], entityGraphWithScoreEncoder)
  implicit val clientResponseEntityGraphWithScoreEncoder: Encoder[ClientResponseWrapper[DocumentId, EntityGraph, EntityGraphWithScore]]
  = Encoders.kryo[ClientResponseWrapper[DocumentId, EntityGraph, EntityGraphWithScore]]
  implicit val documentIdEntityGraphScoreTotalEncoder: Encoder[(DocumentId, EntityGraph, Float)]
  = Encoders.tuple(Encoders.kryo[DocumentId],  entityGraphEncoder, Encoders.scalaFloat)
  implicit val clientResponseInitialiseGraphResponseEncoder: Encoder[ClientResponseWrapper[DocumentId, EntityGraphWithScore, InitialiseGraphResponse]]
  = Encoders.kryo[ClientResponseWrapper[DocumentId, EntityGraphWithScore, InitialiseGraphResponse]]
  implicit val clientResponseInitialiseGraphEntityGraphResponseEncoder: Encoder[ClientResponseWrapper[DocumentId, EntityGraph, InitialiseGraphResponse]]
  = Encoders.kryo[ClientResponseWrapper[DocumentId, EntityGraph, InitialiseGraphResponse]]
  implicit val clientResponseTaskResponseEncoder: Encoder[ClientResponseWrapper[DocumentId, InitialiseGraphResponse, TaskAPI.TaskResponse]]
  = Encoders.kryo[ClientResponseWrapper[DocumentId, InitialiseGraphResponse, TaskAPI.TaskResponse]]
  implicit val clientResponseWorkIdEncoder: Encoder[ClientResponseWrapper[DocumentId, InitialiseGraphResponse, WorkId]]
  = Encoders.kryo[ClientResponseWrapper[DocumentId, InitialiseGraphResponse, WorkId]]

  implicit val bulkOneHopResponseEncoder = Encoders.kryo[BulkRequesterResponse[LiteGraph]]
  implicit val bulkOneHopResponseEncoderWithTiming = Encoders.kryo[BulkRequesterResponseWithTiming[LiteGraph]]
  implicit val documentIdEncoder = Encoders.kryo[DocumentId]

}
