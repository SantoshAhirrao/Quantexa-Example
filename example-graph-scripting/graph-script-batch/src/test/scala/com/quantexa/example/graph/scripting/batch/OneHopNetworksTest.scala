package com.quantexa.example.graph.scripting.batch

import com.quantexa.example.graph.scripting.GraphScriptingConfig
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders._
import com.quantexa.example.graph.scripting.batch.rest.SparkTestSuite
import com.quantexa.example.graph.scripting.stages.CustomerOneHopExpansionStage
import com.quantexa.graph.script.spark.StageContext
import com.quantexa.graph.script.timeouts.StageTimeout
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.typesafe.config.ConfigFactory
import io.circe.config.syntax.CirceConfigOps
import io.circe.generic.auto._
import io.circe.config.syntax._
import com.quantexa.graph.script.spark.RestGraphScriptingDatasetExtensions._
import com.quantexa.graph.script.tags.GraphScriptTest

import scala.concurrent.ExecutionContext

@GraphScriptTest
class OneHopNetworksTest extends SparkTestSuite {

  "One hop networks proto client mode" should "return all successful outputs" in {

    import spark.implicits._

    /* LOAD CONFIG */

    //If gateway location is not set, default config will be used from reference.conf
    lazy val gatewayLocation = Option(System.getProperty("gateway.location")).getOrElse("")

    val testGraphscriptConf: GraphScriptingConfig = ConfigFactory
      .load(s"environment-$gatewayLocation")
      .getConfig("graphscript")
      .as[GraphScriptingConfig] match {
      case Left(error) => throw new IllegalStateException(s"Unable to load config reference.conf\n Error: ${error.getLocalizedMessage}")
      case Right(conf) => conf
    }
    val stageTimeout: StageTimeout = StageTimeout(testGraphscriptConf.timeouts.stageTimeout.get)
    val clientFactorySettings = ClientFactorySettings(testGraphscriptConf.gatewayUri,
      testGraphscriptConf.username,
      testGraphscriptConf.password,
      testGraphscriptConf.resolverConfigPath,
      stageTimeout)

    /* LOAD INPUT */
    val docIds = Seq(
      new DocumentId("customer", "1"),
      new DocumentId("customer", "2"),
      new DocumentId("customer", "3"),
      new DocumentId("customer", "4"),
      new DocumentId("customer", "5")
    ).toDS.map(docId => (docId, docId))

    /* DEFINE STAGE */
    val stage =
      (ctx: StageContext[DocumentId, DocumentId])
      => CustomerOneHopExpansionStage(ctx.input, Some(testGraphscriptConf), ctx.clientFactory)(ExecutionContext.global)

    /* RUN GRAPH SCRIPT */
    val output = docIds.mapWithClient[EntityGraph](clientFactorySettings, stage)

    /* SEPARATE SUCCESSES AND FAILURES */
    val successes = output.filter(_.outputData.isDefined)
    val fails = output.filter(_.errorMessage.isDefined)

    /* TESTS */
    assert(output.count == 5)
    assert(successes.count == 5)
    assert(fails.count == 0)
  }
}
