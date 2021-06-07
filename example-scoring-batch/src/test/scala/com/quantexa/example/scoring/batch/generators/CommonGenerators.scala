package com.quantexa.example.scoring.batch.generators

import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper

object CommonGenerators {
  case class Document(
                       document_type: Option[String],
                       document_id: Option[String])

  object Document {
    val generator = for {
      document_type <- GeneratorHelper.maybeStringGen
      document_id <- GeneratorHelper.maybeStringGen
    } yield Document(
      document_type,
      document_id)
  }
}

