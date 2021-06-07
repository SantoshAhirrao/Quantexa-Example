package com.quantexa.example.scoring.batch.generators

import java.sql.Date

import com.quantexa.example.scoring.batch.generators.CommonGenerators.Document
import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper
import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper._
import com.quantexa.example.model.fiu.scoring.EntityAttributes.Individual
import org.scalacheck.Gen
import org.scalacheck.Gen.chooseNum

import scala.reflect.ClassTag
case class LinkDataAttributes(
                               total_records: Option[Int],
                               individualOnHotlist: Option[String])

object LinkDataAttributes {
  val generator = for {
    total_records <- Gen.some(Gen.chooseNum(0, 1000000))
    individualOnHotlist <- someStringGen
  } yield LinkDataAttributes(
         total_records,
      individualOnHotlist)}

case class LinkData(
                     document: Option[Document],
                     record_ids: Seq[String],
                     excluded: Option[Boolean],
                     attributes: Option[LinkDataAttributes])

object LinkData {

  val generator = for {
    document <- Gen.some(Document.generator)
    record_ids <- Gen.containerOfN[Seq, String](10,Gen.alphaNumStr)
    excluded <- maybeBooleanGen
    attributes <- Gen.some(LinkDataAttributes.generator)
  } yield LinkData(
    document,
    record_ids,
    excluded,
    attributes
  )
}


object IndividualEntity {

  val generator = for {
    customerStatus <-  Gen.option(Gen.oneOf("A", "C"))
    fullName <- Gen.some(fullnameGen)
    residenceCountry <- Gen.some(countryGen)
    customerRiskRating <- Gen.some(chooseNum(0.toLong, 52000.toLong))
    customerStartDateLong <- Gen.some(javaSqlDateGenerator(defaultStartDate, defaultDateRange))
    customerEndDateLong <- Gen.some(javaSqlDateGenerator(defaultStartDate.plus(defaultDateRange), defaultDateRange))
    personalNationalIdNumber <- Gen.some(idGen)
    employeeFlag <- maybeBooleanGen.map(_.map(_.toString))
    nationality <- Gen.some(countryIsoGen)
    gender <- Gen.some(genderGen)
    customerIdNumber <-  Gen.some(idGen)
  } yield
    Individual(
      customerStartDateLong,
      customerRiskRating,
      customerEndDateLong,
      nationality,
      personalNationalIdNumber,
      fullName,
      gender,
      employeeFlag,
      residenceCountry,
      customerIdNumber,
      customerStatus)
}

case class SubEntity(
                      sub_entity_id: Option[String],
                      documents: Seq[Document],
                      valid_from: Option[Date],
                      valid_to: Option[Date],
                      excluded: Option[Boolean],
                      attributes: Option[Individual])

object SubEntity {

  val generator = for {
    sub_entity_id <- Gen.some(idGen)
    documents <- Gen.containerOfN[Seq, Document](10, Document.generator)
    valid_from <- Gen.some(javaSqlDateGenerator(defaultStartDate, defaultDateRange))
    valid_to <- Gen.some(javaSqlDateGenerator(defaultStartDate.plus(defaultDateRange), defaultDateRange))
    excluded <- GeneratorHelper.maybeBooleanGen
    attributes <- Gen.some(IndividualEntity.generator)
  } yield SubEntity(
    sub_entity_id,
    documents,
    valid_from,
    valid_to,
    excluded,
    attributes)
}

case class IndividualEng(
                          entity_id: Option[String],
                          label: Option[String],
                          excluded: Option[Boolean],
                          attributes: Option[Individual],
                          link_data: Seq[LinkData],
                          sub_entities: Seq[SubEntity])

case object IndividualGenerator extends GeneratesDataset[IndividualEng] {

  def generate(implicit ct: ClassTag[IndividualEng]): Gen[IndividualEng] = for {
    entity_id <- Gen.some(Gen.alphaStr)
    label <- Gen.some(Gen.alphaStr)
    excluded <- maybeBooleanGen
    attributes <- Gen.some(IndividualEntity.generator)
    link_data <- Gen.containerOfN[Seq, LinkData](10, LinkData.generator)
    sub_entities <- Gen.containerOfN[Seq, SubEntity](10, SubEntity.generator)
  } yield
    IndividualEng(
      entity_id,
      label,
      excluded,
      attributes,
      link_data,
      sub_entities)
}
