package com.quantexa.example.taskloading.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationContext, DeserializationFeature, JsonDeserializer, ObjectMapper}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.quantexa.resolver.core.EntityGraph.{DocumentId, DocumentIdKeyDeserializer}

/**
  * We use the mapper in this object for decoding/deserializing json
  */
object Jackson {

  class DocumentIdDeserializer extends JsonDeserializer[DocumentId] {
    override def deserialize(jsonParser: JsonParser, context: DeserializationContext): DocumentId = {
      val string = jsonParser.readValueAs[String](classOf[String])
      val split = string.split("-")
      new DocumentId(split(0), split(1))
    }
  }

  val customJacksonModule = new SimpleModule("CustomJackson")
  customJacksonModule.addKeyDeserializer(classOf[DocumentId], new DocumentIdKeyDeserializer)
  customJacksonModule.addDeserializer(classOf[DocumentId], new DocumentIdDeserializer)

  val objectMapper = new ObjectMapper
  objectMapper.registerModule(DefaultScalaModule)
  objectMapper.registerModule(new JodaModule)
  objectMapper.registerModule(customJacksonModule)
  objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

}
