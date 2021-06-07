package com.quantexa.example.graph.scripting.batch.rest

import com.quantexa.graph.script.utils.BulkRequesterAPI.BulkRequesterResponse
import com.quantexa.graph.script.tags.GraphScriptTest
import com.typesafe.config.ConfigFactory
import com.quantexa.example.graph.scripting.GraphScriptBulkLoaderConfig
import com.quantexa.resolver.core.EntityGraph.DocumentId
import org.apache.spark.sql.Dataset
import io.circe.generic.auto._
import io.circe.config.syntax.CirceConfigOps
import io.circe.config.syntax._
import io.circe.generic.auto.exportDecoder
import com.quantexa.example.graph.scripting.batch.encoders.CirceEncoders._
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders._
import com.quantexa.graph.script.utils.{BulkRequestHandler, RestScalaClient, SslContexts}
import com.quantexa.graph.script.utils.BulkRequestHandler.BulkRequestConfig
import com.quantexa.graph.script.utils.BulkRequesterAPI.BulkRequesterSuccess
import com.quantexa.graph.script.utils.GenericDerivation._
import com.typesafe.config.ConfigFactory
import com.quantexa.resolver.core.EntityGraphLite.LiteGraph


@GraphScriptTest
class BulkOneHopTest extends SparkTestSuite {

  import spark.implicits._

  //If gateway location is not set, default will be used from reference.conf
  lazy val gatewayLocation = Option(System.getProperty("gateway.location")).getOrElse("")

  //Load config
  lazy val bulkLoaderConfig: GraphScriptBulkLoaderConfig = ConfigFactory
    .load(s"environment-$gatewayLocation")
    .getConfig("bulkloader")
    .as[GraphScriptBulkLoaderConfig] match {
    case Left(error) => throw new IllegalStateException(s"Unable to load config from location: $gatewayLocation. Error: ${error.getMessage}")
    case Right(config) => config
  }

  //Set properties needed for tests.
  lazy val gatewayUrl: String = bulkLoaderConfig.url.getOrElse(throw new IllegalStateException(s"Gateway url not defined in config."))
  lazy val loginUrl = s"$gatewayUrl/api/authenticate"
  lazy val postUrl = s"$gatewayUrl/api/app-graph-script/graph-scripting/one-hop-customer/"
  lazy val getUrl = s"$gatewayUrl/api/app-graph-script/graph-scripting/one-hop-customer-check/"
  lazy val username: String = bulkLoaderConfig.username
  lazy val password: String = bulkLoaderConfig.password

  //Run graph script only when needed!
  lazy val docIdDS: Dataset[DocumentId] = Seq(new DocumentId("customer", "29986986656")).toDS()
  lazy val output = BulkRequestHandler.submit[DocumentId, LiteGraph](
    docIdDS,
    BulkRequestConfig(loginUrl, postUrl, getUrl, username, password),
    RestScalaClient.httpClient(secure = false))
  lazy val response: BulkRequesterResponse[LiteGraph] = output.head.bulkRequesterResponse

  "Expand one hop networks graph script" should "return a BulkRequesterSuccess type object." in {
    assert(response.isInstanceOf[BulkRequesterSuccess[LiteGraph]])
  }

  lazy val liteGraph = response.asInstanceOf[BulkRequesterSuccess[LiteGraph]].output

  "The graph script output" should "contain 5 entities" in {
    liteGraph.entities.size shouldEqual 5
  }
  it should "contain 1 document" in {
    liteGraph.docs.size shouldEqual 1
  }
}

