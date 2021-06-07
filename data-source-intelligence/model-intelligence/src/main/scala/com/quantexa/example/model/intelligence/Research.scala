package com.quantexa.example.model.intelligence

import com.quantexa.intelligence.api.IntelExtractorImplicits._
import com.quantexa.intelligence.api.UserDefinedIntelligenceModel.{IntelDocumentExtractor, _}
import org.joda.time.DateTime

object ResearchDocumentType extends IntelDocType("research")

case class Research(
                     label: String,
                     `type`: String = ResearchDocumentType.value,
                     startDate: Option[DateTime] = None,
                     endDate: Option[DateTime] = None,
                     comments: Option[String] = None,
                     articleLinks: Option[Seq[String]] = None,
                     icon: Option[String] = None) extends IntelDocument

object ResearchExtractor extends IntelDocumentExtractor[Research](ResearchDocumentType.value)
