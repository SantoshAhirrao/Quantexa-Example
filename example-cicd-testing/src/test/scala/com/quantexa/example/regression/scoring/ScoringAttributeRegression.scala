package com.quantexa.example.regression.scoring

import com.quantexa.etl.core.elastic.{Client, ElasticLoaderFullSettings}
import com.quantexa.etl.test.tags.CICDTest
import com.quantexa.example.model.fiu.scoring.EntityAttributes._
import org.scalatest.{FlatSpec, Matchers}
import com.typesafe.config.{ConfigFactory => cf}
import io.circe.config.syntax.CirceConfigOps
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import com.quantexa.elastic.api.{BooleanQuery, ExistsQuery, Index, SearchQuery}
import com.quantexa.elastic.client.QElasticClient
import com.quantexa.example.model.fiu.scoring.DocumentAttributes._
import monix.eval.Task

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.reflect.runtime.universe._
import scala.collection.JavaConversions._
import monix.execution.Scheduler.Implicits.global

@CICDTest
class ScoringAttributeRegression extends FlatSpec with Matchers {

  import ScoringAttributeRegressionConfiguration._

  "ETL Pipeline" should "create Business attributes required for Scoring" in {
    val resolverIndicesToCheck = resolverIndicesFromConfig.filterNot(_.contains("doc2rec")).filter(_.contains("business"))

    withClue("List of missing Business attributes: ") {
      assert(calculateScoringAttributeCounts[Business](resolverIndicesToCheck).collect { case (attr, 0) => attr }.isEmpty)
    }
  }

  it should "create Individual attributes required for Scoring" in {
    val resolverIndicesToCheck = resolverIndicesFromConfig.filterNot(_.contains("doc2rec")).filter(_.contains("individual"))

    withClue("List of missing Individual attributes: ") {
      assert(calculateScoringAttributeCounts[Individual](resolverIndicesToCheck).collect { case (attr, 0) => attr }.isEmpty)
    }
  }

  it should "create Address attributes required for Scoring" in {
    val resolverIndicesToCheck = resolverIndicesFromConfig.filterNot(_.contains("doc2rec")).filter(_.contains("address"))

    withClue("List of missing Address attributes: ") {
      assert(calculateScoringAttributeCounts[Address](resolverIndicesToCheck).collect { case (attr, 0) => attr }.isEmpty)
    }
  }

  it should "create Telephone attributes required for Scoring" in {
    val resolverIndicesToCheck = resolverIndicesFromConfig.filterNot(_.contains("doc2rec")).filter(_.contains("telephone"))
    withClue("List of missing Telephone attributes: ") {
      assert(calculateScoringAttributeCounts[Telephone](resolverIndicesToCheck).collect { case (attr, 0) => attr }.isEmpty)
    }
  }

  it should "create Account attributes required for Scoring" in {
    val resolverIndicesToCheck = resolverIndicesFromConfig.filterNot(_.contains("doc2rec")).filter(_.contains("account"))

    withClue("List of missing Account attributes: ") {
      assert(calculateScoringAttributeCounts[Account](resolverIndicesToCheck).collect { case (attr, 0) => attr }.isEmpty)
    }
  }

  it should "create Customer Document Attributes required for Scoring" in {
    val resolverIndicesToCheck = resolverIndicesFromConfig.filter(_.contains("customer-doc2rec"))

    withClue("List of missing Customer Document attributes: ") {
      assert(calculateScoringAttributeCounts[CustomerAttributes](resolverIndicesToCheck).collect { case (attr, 0) => attr }.isEmpty)
    }
  }

  it should "create Hostlist Document Attributes required for Scoring" in {
    val resolverIndicesToCheck = resolverIndicesFromConfig.filter(_.contains("hotlist-doc2rec"))

    withClue("List of missing Hostlist Document attributes: ") {
      assert(calculateScoringAttributeCounts[HotlistAttributes](resolverIndicesToCheck).collect { case (attr, 0) => attr }.isEmpty)
    }
  }
}

object ScoringAttributeRegressionConfiguration {

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  private val config = cf.parseResources("scoringAttributeElasticTest.conf").resolve

  val resolverIndicesFromConfig: Seq[String] = if (config.hasPath("scoringAttributeTest.resolverIndices")) {
    config.getStringList("scoringAttributeTest.resolverIndices").toList
  } else throw new IllegalArgumentException("Config is missing under: scoringAttributeTest.resolverIndices")

  private val timeoutInSeconds: Int = if (config.hasPath("scoringAttributeTest.timeoutInSeconds")) {
    config.getInt("scoringAttributeTest.timeoutInSeconds")
  } else throw new IllegalArgumentException("Config is missing under: scoringAttributeTest.timeoutInSeconds")

  private val parsedConfig: ElasticLoaderFullSettings = config.as[ElasticLoaderFullSettings]("elastic") match {
    case Left(error) => throw new IllegalArgumentException(error)
    case Right(elasticLoaderFullSettings) => elasticLoaderFullSettings
  }

  private val qElasticClient: QElasticClient = Client.buildClient(parsedConfig, "resolver")

  private def getAttributeCount(indices: Seq[String], attributeName: String): Long = {
    val searchQuery = SearchQuery(
      indexes = indices.map(Index),
      query = Some(BooleanQuery.boolQuery.must(Seq(ExistsQuery(s"attributes.$attributeName")))),
      size = Some(0))

    val taskSearchQuery: Task[Long] = qElasticClient.search(searchQuery, None).map(_.total)

    Await.result(taskSearchQuery.runToFuture, timeoutInSeconds.seconds)
  }

  private def classAccessors[T: TypeTag]: List[MethodSymbol] = typeOf[T].members.collect {
    case m: MethodSymbol if m.isCaseAccessor => m
  }.toList

  /**
    * @tparam T Case class of field names to check
    * @param resolverIndicesToCheck List of resolver indices to check for attribute existence
    * @return counts of attributes populated for the case class' field names in the provided list of resolver indices
    */
  def calculateScoringAttributeCounts[T <: Product: TypeTag](resolverIndicesToCheck: Seq[String]): Map[String, Long] = {
    val attributeList = classAccessors[T].map(_.fullName.split("\\.").last)

    attributeList.foldLeft(Map.empty[String, Long]) { case (attributeToCountMap, attributeName) =>
      attributeToCountMap + (attributeName -> getAttributeCount(resolverIndicesToCheck, attributeName))
    }
  }
}