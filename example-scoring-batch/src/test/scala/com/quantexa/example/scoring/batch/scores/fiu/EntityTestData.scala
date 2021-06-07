package com.quantexa.example.scoring.batch.scores.fiu

import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.batch.scores.fiu.DocumentTestData.customerDocs
import com.quantexa.example.model.fiu.scoring.EdgeAttributes.IndividualEdge
import com.quantexa.example.model.fiu.scoring.EntityAttributes.Individual
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
object EntityTestData {
  val aDate = DocumentTestData.aDate

  val testInd = Individual(
       customerStartDateLong=Some(aDate),
       customerRiskRating=Some(45123),
       customerEndDateLong=Some(aDate),
       nationality=Some("French"),
       personalNationalIdNumber=Some("1b2vv2"),
       fullName=Some("Mister Pink"),
       gender=Some("Male"),
       employeeFlag=Some("false"),
       residenceCountry=Some("US"),
       customerIdNumber=Some("1234"),
       customerStatus=Some("A"))

  val individuals = Map(
    "E0" -> testInd).toSeq

  private val edges = Seq(("D0", "E0"), ("D1", "E0"), ("D2", "E0"), ("D3", "E1"))
  private val edgeMap = edges.groupBy { case (docId, entId) => entId }.mapValues(_.map(_._1))

  case class EntityType(entityID: String, entityType: String)
  case class EngDocumentId(document_type: String, document_id: String)
  case class LinkData(document: EngDocumentId, record_ids: Seq[String], excluded:Boolean,attributes:IndividualEdge)

  def documentDS(spark: SparkSession): Dataset[Customer] = {
    import spark.implicits._
    spark.createDataset(customerDocs)
  }

  def entityAttributesDF(spark: SparkSession): DataFrame = {
    import spark.implicits._
    spark
      .createDataset(individuals)
      .map {
        case (entId, entAttrs) =>
          val linkData = edgeMap(entId).map(docId => LinkData(EngDocumentId("customer", docId),Seq("1"),false,IndividualEdge(Some(1),Some("true"))))
          (entId, "label", false, entAttrs, linkData)
      }
      .toDF(
        "entity_id",
        "label",
        "excluded",
        "attributes",
        "link_data")
  }

}