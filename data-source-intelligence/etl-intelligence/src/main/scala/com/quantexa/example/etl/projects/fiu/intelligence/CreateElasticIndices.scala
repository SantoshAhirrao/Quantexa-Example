package com.quantexa.example.etl.projects.fiu.intelligence

import java.util.concurrent.TimeUnit

import scala.reflect.ClassTag
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import com.quantexa.elastic.client.{Elastic6Client, QElasticClient}
import com.quantexa.elastic.loader.api.ClassTagAndChildTags
import com.quantexa.elastic.loader.api.TagAndPath
import com.quantexa.elastic.loader.utils.index.IndexDefinitionUtils
import com.quantexa.elastic.loader.utils.settings.{ElasticDataModelSettingsImpl, ElasticLoaderSettings, IndexOptions, ResolverIndexOptions}
import com.quantexa.etl.core.elastic.Client
import com.quantexa.etl.core.elastic.ElasticLoaderFullSettings
import com.quantexa.example.model.intelligence.Research
import com.quantexa.example.model.intelligence.ResearchDocumentType
import com.quantexa.intelligence.api.UserDefinedIntelligenceModel.IndexableIntelDocumentMetadata
import com.quantexa.resolver.core.EntityGraph.EntityType
import com.quantexa.scriptrunner.QuantexaSparkScript
import com.quantexa.scriptrunner.TypedSparkScript
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

object CreateElasticIndices extends TypedSparkScript[ElasticLoaderFullSettings]()(ConfigDecoder.loaderDecoder) {
  val name: String = "Create Empty Intelligence Indices"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  val EntityTypes = Seq("business", "individual", "address", "account", "telephone")
  val IndexSuffix = s"-fiu-smoke-${ResearchDocumentType.value}"
  val ClassTags = ClassTagAndChildTags[IndexableIntelDocumentMetadata[Research]](
    ClassTag(classOf[IndexableIntelDocumentMetadata[Research]]),
    List(TagAndPath(ClassTag(classOf[Research]), "document")))

  def run(spark: SparkSession, logger: Logger, args: Seq[String], config: ElasticLoaderFullSettings, etlMetricsRepository: ETLMetricsRepository) = {
    val searchClient = Client.buildClient(config, "search")
    val resolverClient = Client.buildClient(config, "resolver")
    try {
      Await.result(
        IndexDefinitionUtils.createOrUpdateIndexDefinitions(
          loaderSettings(config.index.prefix, searchClient, resolverClient), ClassTags).runToFuture,
        FiniteDuration(5, TimeUnit.MINUTES)
      )
    } finally {
      searchClient.close()
      resolverClient.close()
    }
  }

  private def loaderSettings(indexPrefix: String, searchClient: QElasticClient, resolverClient: QElasticClient) = {

    val prefix = if(indexPrefix.isEmpty) indexPrefix else s"$indexPrefix-"

    val searchIndexOptions = IndexOptions(s"${prefix}search-research", 1, 0, Map.empty, searchClient)
    val resolverIndexOptions = IndexOptions(s"${prefix}resolver-research-doc2rec", 1, 0, Map.empty, resolverClient)
    val recordIndices = EntityTypes.map(entity => (new EntityType(entity), IndexOptions(s"${prefix}resolver-research-$entity", 1, 0, Map.empty, resolverClient))).toMap

    ElasticLoaderSettings(
      documentType = ResearchDocumentType.value,
      searchIndex = searchIndexOptions,
      resolverIndex = ResolverIndexOptions(resolverIndexOptions, recordIndices),
      searchMappingSettings = Some(ElasticDataModelSettingsImpl(ClassTags)))
  }
}

object ConfigDecoder {
  implicit val customConfig: Configuration = Configuration.default.withDefaults
  val loaderDecoder = exportDecoder[ElasticLoaderFullSettings].instance
}