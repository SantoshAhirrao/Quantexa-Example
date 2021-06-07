package com.quantexa.example.etl.projects.fiu

import com.quantexa.example.etl.projects.fiu.utils.ElasticTestClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.IndexesAndTypes
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.searches.SearchRequest
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
@Category(Array(classOf[ElasticsearchIntegrationTest]))
class ElasticsearchIntegrationTest extends FlatSpec with Matchers {

  val elastic4sClient: ElasticClient = ElasticTestClient.elastic4sClient

  "ElasticsearchIntegrationTest" should "Be able to contact search-fiu-smoke index" in {
    val searchDefinition = SearchRequest(IndexesAndTypes("search-fiu-smoke", "customer"))
    Await.result(elastic4sClient.execute(searchDefinition.scroll(keepAlive = "1m")), Duration.Inf).result.isTimedOut shouldBe false
  }

  it should "Be able to contact resolver-fiu-smoke index" in {
    val searchDefinition = SearchRequest(IndexesAndTypes("resolver-fiu-smoke", "customer"))
    Await.result(elastic4sClient.execute(searchDefinition.scroll(keepAlive = "1m")), Duration.Inf).result.isTimedOut shouldBe false
  }

  it should "Be able to get back data from search-fiu-smoke" in {
    val searchDefinition = SearchRequest(IndexesAndTypes("search-fiu-smoke", "customer"))
    Await.result(elastic4sClient.execute(searchDefinition.scroll(keepAlive = "1m")), Duration.Inf).result.isEmpty shouldBe false
  }
}