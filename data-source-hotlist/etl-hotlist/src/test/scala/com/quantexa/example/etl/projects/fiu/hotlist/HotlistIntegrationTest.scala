package com.quantexa.example.etl.projects.fiu.hotlist

import com.quantexa.example.etl.projects.fiu.hotlist.HotlistTestUtils.writeHotlistFilesCSV
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils.deleteDir
import com.quantexa.example.etl.projects.fiu.utils.SharedTestUtils.ETLConfig
import com.quantexa.example.etl.projects.fiu.utils.{IntegrationTest, SharedTestUtils}
import com.quantexa.scriptrunner.util.DevelopmentConventions.FolderConventions
import org.scalatest.{BeforeAndAfterAll, Tag}

class HotlistIntegrationTest extends IntegrationTest with BeforeAndAfterAll {

  import spark.implicits._
  val ds = "hotlist"

  lazy val indexName: String = getIndexName(ds)
  lazy val dataRootDir: String = getClass.getResource("/").getPath + "data"
  lazy val hotlistRootDir = s"$dataRootDir/$ds"
  var resolverIndices: Seq[String] = Seq()

  //These parts written inside a test that is always true, in order to permit use of tagging to exclude computation
  "Hotlist Integration Test" should "always test true here" taggedAs Tag("SlowTest") in {
    val etlConfig = ETLConfig(dataRootDir, None)
    val loaderConfig = SharedTestUtils.createLoaderConfig(hotlistRootDir, indexName)

    HotlistTestUtils.extractTransform(spark, logger, etlConfig, metrics, dataRootDir)
    LoadElastic.run(spark, logger, args = Seq(), loaderConfig, metrics)

    val entityTypes = spark.read.parquet(FolderConventions.entityTypesFile(hotlistRootDir)).as[String].collect.toSeq
    resolverIndices = (entityTypes ++ Seq("doc2rec")).map(str => s"resolver-$indexName-$str")

    assert(true)
  }

  it should "create at least 1 resolver index" taggedAs Tag("SlowTest") in {
    resolverIndices.size should be > 0
  }

  it should s"create and load resolver indices, each with 1 document mapping" taggedAs Tag("SlowTest") in {
    val resolverMappingsCount = resolverIndices.map(index => getMappingsCount(index))
    resolverMappingsCount shouldBe Seq.fill[Int](resolverIndices.size)(1)
  }

  it should "create and load no more than 1 Elastic Document type to the Search Index" taggedAs Tag("SlowTest") in {
    val searchMappingsCount = getMappingsCount(s"search-$indexName")
    searchMappingsCount shouldBe 1
  }

  it should "only load Search and Resolve indices to Elasticsearch" taggedAs Tag("SlowTest") in {
    val hotlistIndices = listIndices(s".*-$ds*").sorted
    hotlistIndices shouldBe (Seq(s"search-$indexName") ++ resolverIndices).sorted
  }

  it should "extract Transform and Load the correct number of documents to the Resolver Index" taggedAs Tag("SlowTest") in {
    val resolverDocumentCount: Int = resolverIndices.map(index => getIndexCount(index)).sum
    resolverDocumentCount shouldBe 20
  }

  it should "extract Transform and Load the correct number of documents to the Search Index" taggedAs Tag("SlowTest") in {
    val searchDocumentCount: Int = getIndexCount(s"search-$indexName")
    searchDocumentCount shouldBe 9
  }

  override def beforeAll(): Unit = {
    writeHotlistFilesCSV(dataRootDir)
  }

  override def afterAll(): Unit = {
    deleteDir(dataRootDir)
    SharedTestUtils.dataCleanUp(hotlistRootDir)
  }
}