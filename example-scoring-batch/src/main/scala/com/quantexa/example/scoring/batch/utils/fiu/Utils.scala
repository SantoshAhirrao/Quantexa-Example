package com.quantexa.example.scoring.batch.utils.fiu

import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreDefinition.AbstractScoreTypes
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

object Utils {

  lazy val spark = SparkSession.
    builder().
    appName("Quantexa ETL").
    enableHiveSupport().
    getOrCreate()

  import spark.implicits._

  case class id() extends scala.annotation.StaticAnnotation
  case class CustomerRolldown(@id cus_id_no_string: String)
  case class AggregateScoreEntry(document_id: String, severity: Option[Long])
  case class TransformedAggregateScoreEntry(id : CustomerRolldown, basicScoreOutput: Option[BasicScoreOutput])


  def explodeScoreOutput(df: DataFrame, colName: String): DataFrame = df.select(col(colName).as("id"), explode($"basicScoreOutputMap")).select('id, 'key, $"value.*")

  def pivotedScoreOutput(df: DataFrame, colName: String): DataFrame = {

    def aliasCols(t: DataFrame): DataFrame = t.toDF(t.columns.map(mapStr(_, "_max")): _*)

    def mapStr(s: String, c: String): String = s.split(c) match { case Array(_, l2, _, _*) => l2; case Array(l1, l2, _*) => mapStr(l2, "`") + " (" + l1 + ")"; case _ => s }

    aliasCols(explodeScoreOutput(df, colName).groupBy("id").pivot("key").agg(max("severity"), max("band"), max("description")))
  }

  def round(number: Double, precision: Int): Double = BigDecimal(number).setScale(precision, BigDecimal.RoundingMode.HALF_UP).toDouble

  def getNetworkTable(path: String): DataFrame = {
    val netOutput = spark.read.parquet(path)

    val net2 = netOutput.withColumn("document_type", netOutput("document")("documentType")).withColumn("document_id", netOutput("document")("documentID"))
    val net3 = net2.withColumn("entity_type", netOutput("entity")("entityType")).withColumn("entity_id", netOutput("entity")("entityID")).withColumn("sub_entity_id", netOutput("entity")("subEntityID"))
    val net4 = net3.drop(net3("entity")).drop(net3("document")).drop(net3("records"))

    net4.select("network_id", "document_id", "entity_type", "entity_id").orderBy("network_id", "document_id", "entity_type", "entity_id")

  }
  //FIXME: IP-513 This should come from the Scoring Framework.
  def inferPathFromScore(config: ProjectExampleConfig, score: AbstractScoreTypes): String = {
    val scoreDetails = score.toString().split("\\.")
    val folder = scoreDetails(0).split("(?<=.)(?=\\p{Lu})")(0)

    val filename = score.toString().split("TypeTag").drop(1)
      .map(_.split("\\W+").reverse(0).split("(?<=.)(?=\\p{Lu})")(0) concat ("_"))
      .dropRight(1).mkString("").dropRight(1)

    println(config.hdfsFolder + "/Scoring/" + folder + "/" + filename + "/")
    config.hdfsFolder + "/Scoring/" + folder + "/" + filename + "/"
  }

  def aggregateSingleEntityScoreType(config: ProjectExampleConfig, input: (AbstractScoreTypes, String), networkTable: DataFrame): DataFrame = {
    val score = input._1.score
    val scoreData = spark.read.parquet(inferPathFromScore(config, input._1))
    val scoreDataFiltered = explodeScoreOutput(scoreData, "id").filter($"key" === score.id)

    val aggregationFun = scoreDataFiltered.select("severity").columns.map((_ -> input._2)).toMap
    val simpleJoin = networkTable.join(scoreDataFiltered, networkTable("entity_id") === scoreDataFiltered("id"), "leftouter").drop("id").drop("network_id").drop("entity_type")

    val scoreTable = simpleJoin.groupBy("document_id").agg(aggregationFun).toDF("document_id", "severity")

    scoreTable.as[AggregateScoreEntry].map{
      case AggregateScoreEntry(id, None) => TransformedAggregateScoreEntry(CustomerRolldown(id), None)
      case AggregateScoreEntry(id, Some(sev))  =>   TransformedAggregateScoreEntry(CustomerRolldown(id), Option(BasicScoreOutput(severity = sev.toInt, description = "[Rolldown] " + score.id)))
    }.toDF("id", "basicScoreOutput")
  }

}





