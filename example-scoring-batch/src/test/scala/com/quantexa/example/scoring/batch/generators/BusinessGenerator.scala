package com.quantexa.example.scoring.batch.generators

import java.sql.Date

import com.quantexa.example.scoring.batch.generators.CommonGenerators.Document
import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper._
import com.quantexa.example.model.fiu.scoring.EntityAttributes.Business
import org.scalacheck.Gen

import scala.reflect.ClassTag

case class BusinessLinkDataAttributes(
    total_records: Option[Int],
    businessOnHotlist: Option[String])

object BusinessLinkDataAttributes {
  val generator = for {
    total_records <- Gen.some(Gen.chooseNum(0, 1000000))
    businessOnHotlist <- maybeBooleanGen.map(_.map(_.toString))
  } yield
    BusinessLinkDataAttributes(
      total_records,
      businessOnHotlist)
}
case class BusinessLinkData (
document: Option[Document],
record_ids: Seq[String],
excluded: Option[Boolean],
attributes: Option[BusinessLinkDataAttributes])

object BusinessLinkData {
  val generator = for {
    document <- Gen.some(Document.generator)
    record_ids <- Gen.containerOfN[Seq, String](10, Gen.alphaNumStr)
    excluded <- maybeBooleanGen
    attributes <-  Gen.some(BusinessLinkDataAttributes.generator)
  } yield BusinessLinkData (
    document,
    record_ids,
    excluded,
    attributes)}

object BusinessAttributes {
  val generator = for {
    businessJoinedDate <- Gen.some(javaSqlDateGenerator(defaultStartDate, defaultDateRange))
    businessLeftDate <- Gen.some(javaSqlDateGenerator(defaultStartDate.plus(defaultDateRange), defaultDateRange))
    businessNameDisplay  <- Gen.some(businessNameGen)
    registeredBusinessName = businessNameDisplay
    registeredBusinessNumber <- someStringGen
    countryOfRegisteredBusiness <- Gen.some(countryGen)

  } yield Business(
    businessLeftDate,
    registeredBusinessName,
    businessNameDisplay,
    registeredBusinessNumber,
    countryOfRegisteredBusiness,
    businessJoinedDate)
}

case class BusinessSubEntity (
 sub_entity_id: Option[String],
 documents: Seq[Document],
 valid_from: Option[Date],
 valid_to: Option[Date],
 excluded: Option[Boolean],
 attributes: Option[Business])

object BusinessSubEntity {
  val generator = for {
    sub_entity_id <- Gen.some(idGen)
    documents <- Gen.containerOfN[Seq, Document](10, Document.generator)
    valid_from <- Gen.some(javaSqlDateGenerator(defaultStartDate, defaultDateRange))
    valid_to <- Gen.some(javaSqlDateGenerator(defaultStartDate.plus(defaultDateRange), defaultDateRange))
    excluded <- maybeBooleanGen
    attributes <-  Gen.some(BusinessAttributes.generator)
  } yield  BusinessSubEntity (
    sub_entity_id,
    documents,
    valid_from,
    valid_to,
    excluded,
    attributes)
}
case class BusinessEng(
                      entity_id: Option[String],
                      label: Option[String],
                      excluded: Option[Boolean],
                      attributes: Option[Business],
                      link_data: Seq[BusinessLinkData],
                      sub_entities: Seq[BusinessSubEntity]
                    )
object BusinessEng {
  val generator = for {
    entity_id <- Gen.some(idGen)
    label <- Gen.some(Gen.alphaStr)
    excluded <- maybeBooleanGen
    attributes <- Gen.some(BusinessAttributes.generator)
    link_data <-  Gen.containerOfN[Seq,BusinessLinkData](10, BusinessLinkData.generator)
    sub_entities <- Gen.containerOfN[Seq,BusinessSubEntity](10, BusinessSubEntity.generator)
  } yield BusinessEng (
    entity_id,
    label,
    excluded,
    attributes,
    link_data,
    sub_entities)
}

case object BusinessGenerator extends GeneratesDataset[BusinessEng] {

  def generate(implicit ct: ClassTag[BusinessEng]): Gen[BusinessEng] = BusinessEng.generator

}
