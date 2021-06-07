package com.quantexa.example.scoring.batch.utils.elastic

import com.quantexa.elastic.client.QElasticClient
import com.quantexa.elastic.loader.utils.settings.{ElasticDataModelSettings, ElasticDataModelSettingsImpl}
import com.quantexa.etl.core.elastic.ElasticLoaderResultModels._
import com.quantexa.etl.core.elastic._
import com.quantexa.etl.core.utils.metrics.{JobMetrics, JobMetricsNoOp}
import org.apache.log4j.Logger

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.concurrent.ExecutionContext.Implicits.global
import com.quantexa.etl.core.utils.elastic.{IndexDefinitionUtils, IndexSettingsUtils, PostLoadUtils, ShardUtils}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, SparkScript}
import io.circe.generic.extras.Configuration
import org.apache.spark.TaskContext
import org.apache.spark.sql.SaveMode.Append
import org.apache.spark.sql.execution.datasources.FilePartition
import org.apache.spark.sql.{Encoder, Row, SparkSession}
import org.elasticsearch.hadoop.cfg.PropertiesSettings
import org.elasticsearch.hadoop.rest.InitializationUtils
import org.elasticsearch.spark.cfg.SparkSettingsManager

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success, Try}
import com.quantexa.elastic.loader.utils.settings.{ElasticDataModelSettings, ElasticDataModelSettingsImpl}
import com.quantexa.etl.core.elastic._
import com.quantexa.resolver.ingest.DataModelUtils._
import com.quantexa.scriptrunner.util.DevelopmentConventions.FolderConventions
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, SparkScript}
import io.circe.config.syntax.CirceConfigOps
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import org.apache.log4j.{Level, LogManager, Logger}
import org.apache.spark.sql.SparkSession

class GenericElasticLoadScript[T <: Product : TypeTag](documentType: String,
                                                            documentIdField: String,
                                                            loader: GenericElasticLoaderCombined,
                                                            mappingSettings: Option[ElasticDataModelSettings[T]] = None) extends SparkScript {

  val name = s"ElasticLoader - $documentType"

  val scriptDependencies = Set.empty[QuantexaSparkScript]
  val fileDependencies = Map.empty[String, String]

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  def run(spark: SparkSession, logger: Logger, args: Seq[String], config: com.typesafe.config.Config, metrics: ETLMetricsRepository): Unit = {

    val defaultMappingSettings = ElasticDataModelSettingsImpl[T](typeOf[T].getTopLevelFields(), typeOf[T].getAllFieldNames().toSet)

    val esMappingSettings = mappingSettings match {
      case Some(settings) => settings
      case None => defaultMappingSettings
    }

    val queryDebug = config.hasPath("queryDebug") && config.getBoolean("queryDebug")

    if (queryDebug) LogManager.getLogger("com.sksamuel").setLevel(Level.DEBUG)

    val isUpdateMode = config.hasPath("updateMode") && config.getBoolean("updateMode")
    val loaderConfig = if (isUpdateMode) config.as[ElasticLoaderUpdateSettings] else config.as[ElasticLoaderFullSettings]

    loaderConfig match {
      case Left(error) => logger.error(s"Could not read ElasticLoaderOptions for full mode", error)
      case Right(parsedConfig) =>
        //TODO: We should have logic here to automatically determine the path to use. Taking HDFS root if it's a lookup that's being loaded, then we should know how to build that path
        val documentPath = parsedConfig.otherOptions.get("fullPathToParquet") match {
          case Some(pathToFile) => pathToFile
          case None => throw new IllegalArgumentException("Please provide the fullPathToParquet in otherOptions in your config")
        }
        loader.load(spark,
          logger,
          documentType,
          documentPath,
          parsedConfig,
          esMappingSettings,
          documentIdField,
          metrics
        )
    }

  }

}