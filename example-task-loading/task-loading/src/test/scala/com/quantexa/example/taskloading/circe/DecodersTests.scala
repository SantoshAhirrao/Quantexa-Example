package com.quantexa.example.taskloading.circe

import org.scalatest.{FlatSpec, Matchers}
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto.exportDecoder
import com.quantexa.example.taskloading.circe.Decoders._
import cats.syntax.either._
import com.quantexa.explorer.tasks.loader.api.TaskLoaderAPI.QueueStatusResponse
import com.quantexa.queue.api.Model.Status

class DecodersTests extends FlatSpec with Matchers {

  "The custom decoders" should "allow for QueueStatusResponse to be successfully decoded" in {

    val queueStatusResponseJSON = parse(
      """{
        |  "status": {
        |    "id": {
        |      "value": "97ac4039-08cb-4dc8-ac8b-947de1fc19cb"
        |    },
        |    "groupId": null,
        |    "items": [
        |      {
        |        "workId": {
        |          "value": "f7c93903-f2f1-4e08-83c9-df4a28ade086"
        |        },
        |        "status": {
        |          "name": "QUEUED"
        |        }
        |      }
        |    ],
        |    "total": 1
        |  }
        |}     """.stripMargin).right.get

    val decodeResult = queueStatusResponseJSON.as[QueueStatusResponse]
    assert(decodeResult.isRight)

  }
}