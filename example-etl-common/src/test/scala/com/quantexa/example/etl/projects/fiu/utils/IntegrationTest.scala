package com.quantexa.example.etl.projects.fiu.utils

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.apache.log4j.Logger
import org.apache.log4j.Level
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import com.sksamuel.elastic4s.http.ElasticDsl._

trait IntegrationTest extends FlatSpec with Matchers {
  lazy val logger = Logger.getLogger("quantexa-spark-script")
  logger.setLevel(Level.INFO)

  lazy val metrics = Metrics.repo
  lazy val elastic4sClient = ElasticTestClient.elastic4sClient
  lazy val client = ElasticTestClient.client
  lazy val spark = SparkTestSession.spark

  System.setProperty("stubParser", "true")

  lazy val isWindows = sys.props.get("os.name").forall(_.toLowerCase.contains("windows"))

  def getIndexName(ds: String): String = s"batch-integration-test-$ds"

  def getMappings(indexName: String): Map[String, Map[String, Any]]  = {
    elastic4sClient.execute {
      getMapping(indexName)
    }.await.result.head.mappings
  }

  def getMappingNames(indexName: String): Set[String] = getMappings(indexName).keySet
  def getMappingsCount(indexName: String): Int = getMappings(indexName).size

  def listIndices(wildcard: String): Seq[String] = {
    val pattern = wildcard.replaceAllLiterally("*",".*")
    elastic4sClient.execute {
      catIndices()
    }.await.result.map(_.index)
      .filter(_.matches(pattern))
  }

  def getIndicesCount(indices: Seq[String]): Int = {
    elastic4sClient.execute {
      search(indices).size(0)
    }.await.result.totalHits.toInt
  }

  def getIndexCount(index: String): Int = {
    client.admin.indices.refresh(new RefreshRequest(index)).get
    getIndicesCount(Seq(index))
  }
}