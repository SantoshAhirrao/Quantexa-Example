package com.quantexa.example.etl.projects.fiu.customer

import com.quantexa.example.etl.projects.fiu.utils.{IntegrationTest, SharedTestUtils}
import com.quantexa.example.etl.projects.fiu.customer.CustomerTestUtils.writeCustomerFilesCSV
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils.deleteDir
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Tag}


class CustomerIntegrationTest extends IntegrationTest with BeforeAndAfterAll {

  val ds = "customer"

  lazy val indexName: String = getIndexName(ds)
  lazy val dataRootDir: String = getClass.getResource("/").getPath + "data"
  lazy val customerRootDir = s"$dataRootDir/$ds"
  var resolverIndices: Seq[String] = Seq()

  //These parts written inside a test that is always true, in order to permit use of tagging to exclude computation
  "Customer Integration Test" should "always test true here" taggedAs Tag("SlowTest") in {

    val firstLoaderConfig = SharedTestUtils.createLoaderConfig(customerRootDir, indexName)

    CustomerTestUtils.extractTransform(spark, logger, metrics, dataRootDir)
    LoadElastic.runIncremental(spark, logger, args = Seq(), firstLoaderConfig, metrics)

    val entityTypes = Seq("account", "address", "business", "individual", "telephone")
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
    val customerIndices = listIndices(s".*-$ds*").sorted
    customerIndices shouldBe (Seq(s"search-$indexName") ++ resolverIndices).sorted
  }

  it should "extract Transform and Load the correct number of documents to the Resolver Index" taggedAs Tag("SlowTest") in {
    val resolverDocumentCount: Int = resolverIndices.map(index => getIndexCount(index)).sum
    resolverDocumentCount shouldBe 24
  }

  it should "extract Transform and Load the correct number of documents to the Search Index" taggedAs Tag("SlowTest") in {
    val searchDocumentCount: Int = getIndexCount(s"search-$indexName")
    searchDocumentCount shouldBe 6
  }

  "Customer Integration Test Incremental" should "always test true here" taggedAs Tag("SlowTest") in {
    val entityTypes = Seq("account", "address", "business", "individual", "telephone")
    val secondLoaderConfig = SharedTestUtils.createLoaderConfig(customerRootDir, indexName, entityTypes)

    CustomerTestUtils.extractTransformIncremental(spark, logger, metrics, dataRootDir)
    LoadElastic.runIncremental(spark, logger, args = Seq(), secondLoaderConfig, metrics)

    //TODO: Add back the elastic deleter once it's been fixed by R&D -- QE-2227
//    DeleteFromElastic.run(spark, logger, args = Seq(), secondLoaderConfig, metrics)

    assert(true)
  }

  it should "Generate a metadata.parquet file with two rows" taggedAs Tag("SlowTest") in {
    val metadata = spark.read.parquet(s"$customerRootDir/metadata.parquet")
    metadata.count shouldBe 2
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
    val customerIndices = listIndices(s".*-$ds*").sorted
    customerIndices shouldBe (Seq(s"search-$indexName") ++ resolverIndices).sorted
  }

  it should "extract Transform and Load the correct number of documents to the Resolver Index" taggedAs Tag("SlowTest") in {
    val resolverDocumentCount: Int = resolverIndices.map(index => getIndexCount(index)).sum
    resolverDocumentCount shouldBe 35
  }

  it should "extract Transform and Load the correct number of documents to the Search Index" taggedAs Tag("SlowTest") in {
    val searchDocumentCount: Int = getIndexCount(s"search-$indexName")
    searchDocumentCount shouldBe 9
  }

  override def beforeAll(): Unit = {
    writeCustomerFilesCSV(dataRootDir)
  }

  override def afterAll(): Unit = {
    deleteDir(dataRootDir)
    sys.props -= ("environment.fileSystemRoot", "environment.inputDataFileSystemRoot")
    ConfigFactory.invalidateCaches()
  }
}