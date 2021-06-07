package com.quantexa.example.graph.scripting.batch

import java.util.concurrent.TimeUnit

import com.quantexa.example.graph.scripting.GraphScriptingConfig
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders._
import com.quantexa.example.graph.scripting.expansiontemplates.CustomDocumentExpansionTemplate
import com.quantexa.example.graph.scripting.stages._
import com.quantexa.graph.script.client.ClientResponseWrapper
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.spark.RestGraphScriptingDatasetExtensions._
import com.quantexa.graph.script.spark.StageContext
import com.quantexa.graph.script.timeouts.StageTimeout
import com.quantexa.investigation.api.GraphCustodianProtocol
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.config.syntax._
import io.circe.generic.auto._
import org.apache.log4j.Logger
import org.apache.spark.sql.SaveMode.Overwrite
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/*
See: "Advanced Topics" section in "Graph Scripting the Definitive Guide". The purpose of this script is to show how to
write a performant graph script that includes multiple calls to the resolver. The idea is to use graph merges to avoid
resolving documents/entities more than once.

If the resolver requests happen sequentially, it is possible to combine them into a single resolver request by specifying
the appropriate selectors/resolution templates. If this is not possible (which may happen due to other client calls
going in between resolver calls), then the approach shown here should be used instead.
 */
object EfficientMultiHopExpansion extends TypedSparkScript[GraphScriptingConfig] {

  def name = "EfficientMultiHopExpansion"

  val fileDependencies = Map.empty[String, String]
  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          log: Logger,
          args: Seq[String],
          config: GraphScriptingConfig,
          etlMetricsRepository: ETLMetricsRepository): Unit = {

    import spark.implicits._

    /* CONFIGURATION */
    assert(config.oneHopStage.isDefined, "Config is missing for CustomerOneHopExpansionStage.")
    assert(config.loadInvestigationStage.isDefined, "Config is missing for LoadInvestigationStage.")
    val stageTimeout: StageTimeout = StageTimeout(config.timeouts.stageTimeout.getOrElse(FiniteDuration(30L, TimeUnit.SECONDS)))
    val inputConfig = config.oneHopStage.get
    val loadInvestigationConfig = config.loadInvestigationStage.get
    val clientFactorySettings = ClientFactorySettings(config.gatewayUri,
      config.username,
      config.password,
      config.resolverConfigPath,
      stageTimeout)

    /* LOAD INPUT */
    val rawInputIds = spark.read.parquet(inputConfig.inputPath)
      .as[DocumentId]
      .map(docId => (docId, docId))

    /* DEFINE STAGE 1*/
    val expansionStage: StageContext[DocumentId, DocumentId] => FutureErrorOrWithMetrics[EntityGraph] =
      context =>
        ExpansionStage(Set(context.input),
          CustomDocumentExpansionTemplate.oneDegreeIndividualsOnly,
          Some(config.timeouts),
          context.clientFactory)(ExecutionContext.global)


    /* RUN STAGE 1 */
    val entityGraphs: Dataset[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]] = rawInputIds.mapWithClient(
      clientFactorySettings, expansionStage)

    /*
    DEFINE STAGE 2

    Stage 2: Find document sources for all documents NOT on the graph output from Stage 1. Filter to keep only US customer
    documents, and then resolve and merge these documents (add them to the graph output from Stage 1). Note that only
    documents NOT on the original graph were submitted to resolver, meaning nothing was resolved twice.

    Note also that the document and resolver steps were placed into the same stage to avoid having to join two datasets.
     */
    val addUSDocumentsToGraphStage: StageContext[DocumentId, EntityGraph] => FutureErrorOrWithMetrics[EntityGraph] =
      context =>
        AddUSDocumentsToGraphStage(context.key,
          context.input,
          context.resolverConfigurationForMerge,
          Some(config),
          context.clientFactory)(ExecutionContext.global)

    /* RUN STAGE 2 */
    val mergedUSCustomerDocuments = entityGraphs.mapWithClient(clientFactorySettings, addUSDocumentsToGraphStage)

    /*
    DEFINE STAGE 3

    Stage 3: Find individual and account records relating to entities NOT on the graph output from Stage 2. Submit these
    to the resolver and merge them to the graph output from Stage 2. Note that only records corresponding to entities
    NOT on the original graph were submitted to resolver, meaning no entities were resolved twice.
     */
    val getIndividualsAndAccountsAssociatedWithUSCustomersStage: StageContext[DocumentId, EntityGraph] => FutureErrorOrWithMetrics[EntityGraph] =
      context =>
        GetIndividualsAndAccountsAssociatedWithUSCustomersStage(context.key,
          context.input,
          context.resolverConfigurationForMerge,
          context.clientFactory)(ExecutionContext.global)

    /* RUN STAGE 3 */
    val finalGraphs = mergedUSCustomerDocuments.mapWithClient(clientFactorySettings, getIndividualsAndAccountsAssociatedWithUSCustomersStage)

    /* DEFINE STAGE 4*/
    val loadInvestigationStage: StageContext[DocumentId, EntityGraph] => FutureErrorOrWithMetrics[GraphCustodianProtocol.InitialiseGraphResponse] =
      context =>
        LoadInvestigationStage.apply(context.key, context.input, context.clientFactory)(ExecutionContext.global)

    /* RUN STAGE 4 */
    val loadedInvestigations = finalGraphs.mapWithClient(clientFactorySettings, loadInvestigationStage)

    loadedInvestigations.write.mode(Overwrite).parquet(loadInvestigationConfig.outputPath)
  }
}
