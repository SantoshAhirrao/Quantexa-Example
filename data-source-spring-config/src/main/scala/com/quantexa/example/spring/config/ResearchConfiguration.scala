package com.quantexa.example.spring.config

import com.quantexa.example.model.intelligence.{ Research, ResearchDocumentType }
import com.quantexa.intelligence.api.UserDefinedIntelligenceModel.IntelDocumentSerialization
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import com.quantexa.intelligence.api.IntelExtractorImplicits._

@Configuration
class ResearchConfiguration {
  @Component
  class ResearchSerialization extends IntelDocumentSerialization[Research](ResearchDocumentType.value)
}
