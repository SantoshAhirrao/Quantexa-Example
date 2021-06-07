package com.quantexa.example.taskloading.circe

import cats.syntax.either._
import com.quantexa.queue.api.Model._
import io.circe.{Decoder, HCursor}

/**
  * Custom Circe decoders required to successfully certain types
  */
object Decoders {

  private def convertStringStatusToStatus(s: String): Either[String, Status] = {
    Status.withName(s).toEither.leftMap(l => l.toList.mkString("; "))
  }

  implicit val decodeStatus: Decoder[Status] = {
    val stringDecoder = new Decoder[String] {
      final def apply(c: HCursor): Decoder.Result[String] = {
        for {
          status <- c.downField("name").as[String]
        } yield {
          status
        }
      }
    }
    stringDecoder.emap(convertStringStatusToStatus)
  }

}

