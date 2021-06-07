package com.quantexa.example.graph.scripting.batch.encoders

import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityId}

object CirceEncoders {
  import java.sql.Date
  import cats.syntax.either._
  import io.circe.{Decoder, Encoder}

  implicit val encodeDate: Encoder[Date] = Encoder.encodeLong.contramap[Date](_.getTime)
  implicit val decodeDate: Decoder[Date] = Decoder.decodeLong.emap {
    long => Either.catchNonFatal(new Date(long)).leftMap(err => s"Could not create date: ${err.getMessage}")
  }

  private def documentIdFromJacksonEncoding(docIdAsString: String): DocumentId = {
    val split = docIdAsString.split("-")
    assert(split.size == 2, s"More than one '-' in supplied string:${docIdAsString}. Cannot be sure of correctly forming document-id")
    new DocumentId(split(0), split(1))
  }

  private def entityIdFromJacksonEncoding(entIdAsString: String): EntityId = {
      EntityId.parse(entIdAsString)
  }

  implicit val decodeJacksonEncodedDocumentId: Decoder[DocumentId] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(documentIdFromJacksonEncoding(str)).leftMap(err => s"Could not create DocumentId: ${err.getMessage}")
  }

  implicit val decodeJacksonEncodedEntityId: Decoder[EntityId] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(entityIdFromJacksonEncoding(str)).leftMap(err => s"Could not create EntityId: ${err.getMessage}")
  }
}
