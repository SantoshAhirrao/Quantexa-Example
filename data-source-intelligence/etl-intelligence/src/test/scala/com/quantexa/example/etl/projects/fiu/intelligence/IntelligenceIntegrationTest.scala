package com.quantexa.example.etl.projects.fiu.intelligence

import com.quantexa.example.etl.projects.fiu.utils.IntegrationTest
import com.quantexa.example.etl.projects.fiu.utils.SharedTestUtils
import io.circe.config.syntax.CirceConfigOps
import com.quantexa.etl.core.elastic.ElasticLoaderFullSettings
import org.scalatest.Tag

class IntelligenceIntegrationTest extends IntegrationTest {

  val indexName = getIndexName("research")
  val entityTypes = CreateElasticIndices.EntityTypes
  val resolverIndices = (entityTypes ++ Seq("doc2rec")).map(str => s"resolver-research-$str")
  val searchIndex = "search-research"

  //These parts written inside a test that is always true, in order to permit use of tagging to exclude computation
  "Intelligence Integration Test" should "always test true here" taggedAs Tag("SlowTest") in {
    val loaderConfig = SharedTestUtils.createLoaderConfig("", indexName)
    implicit val decoder = ConfigDecoder.loaderDecoder
    
    loaderConfig.as[ElasticLoaderFullSettings] match {
      case Left(err) => throw err
      case Right(config) => CreateElasticIndices.run(spark, logger, args = Seq(), config, metrics) 
    }

    assert(true)
  }

  it should s"create and load ${entityTypes.size} resolver indices, each with 1 document mapping" taggedAs Tag("SlowTest") in {
    val resolverMappingsCount = resolverIndices.map(index => getMappingsCount(index))
    resolverMappingsCount shouldBe Seq.fill[Int](resolverIndices.size)(1)
  }

  it should "only load Search and Resolve indices to Elasticsearch" taggedAs Tag("SlowTest") in {
    val indices = listIndices(s".*-research*").sorted
    indices shouldBe (Seq(searchIndex) ++ resolverIndices).sorted
  }
  
  it should "create the Search Index with a single mapping" taggedAs Tag("SlowTest") in {
    val searchMappingCount = getMappingsCount(searchIndex)
    searchMappingCount shouldBe 1
  }
}