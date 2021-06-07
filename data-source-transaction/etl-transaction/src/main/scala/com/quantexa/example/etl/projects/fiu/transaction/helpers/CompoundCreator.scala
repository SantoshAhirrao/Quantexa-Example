package com.quantexa.example.etl.projects.fiu.transaction.helpers

import com.quantexa.etl.compounds.ConfigurationUtilities.getPathFromConfigOrDefault
import com.quantexa.etl.core.compounds.CompoundCreatorSettings
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import com.quantexa.scriptrunner.{QuantexaSparkScript, SparkScriptIncremental}
import com.quantexa.scriptrunner.util.DevelopmentConventions.FolderConventions._
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.typesafe.config.Config
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository

import scala.reflect.runtime.universe.TypeTag
import com.quantexa.scriptrunner.util.incremental.MetaDataRepositoryImpl._

case class CompoundCreator[T <: Product](
                                          extractorCanonical:        String,
                                          documentType:              String,
                                          getRunId:                  Option[T => String] = None,
                                          extractElements:           Boolean             = false,
                                          partitionForCores:         Boolean             = true,
                                          outputMaxDocsPerPartition: Int                 = 50000,
                                          checkForDuplicates:        Boolean             = true)(docIdFn: T => String)(implicit tt: TypeTag[T]) extends SparkScriptIncremental {

  val name = s"CompoundCreator - $documentType"

  val scriptDependencies = Set.empty[QuantexaSparkScript]
  val fileDependencies = Map.empty[String, String]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], config: com.typesafe.config.Config, metrics: ETLMetricsRepository, metadataRunId: MetadataRunId) = {

    if (args.nonEmpty) {
      logger.warn(args.length + " arguments were passed to the script and are being ignored")
    }

    val rootHdfsPath = config.getString("dataRoot")
    val metadataPath = config.getString("metadataPath")

    val dataRoot = if(config.hasPath("runId")){
      //If config contains runId, then rootHdfsPath should already end with the runId.
      rootHdfsPath
    } else {
      //If config does not contain runId, find latest from metadata and append to rootHdfsPath.
      s"$rootHdfsPath/${metadataRunId.runId}"
    }

    val inputFilePath = getPathFromConfigOrDefault("aggregatedCaseClassPath",
      cleansedAggregationCaseClassFile,
      config,
      dataRoot,
      logger)

    val indexInputFilePath = getPathFromConfigOrDefault("indexInputFile",
      indexInputFile,
      config,
      dataRoot,
      logger)

    val entityTypesFilePath = getPathFromConfigOrDefault("entityTypesFile",
      entityTypesFile,
      config,
      dataRoot,
      logger)

    com.quantexa.etl.core.compounds.CompoundCreator[T](
      spark,
      logger,
      getRunIdFunction(getRunId, spark, dataRoot, config),
      CompoundCreatorSettings(extractorCanonical, documentType, inputFilePath, indexInputFilePath,
        entityTypesFilePath, extractElements, partitionForCores, outputMaxDocsPerPartition, checkForDuplicates),
      metrics)(docIdFn)

    metaDataRepository(spark, metadataPath, metadataRunId, name, logger)
  }

  private def getRunIdFunction[T <: Product](
                                              optionalFn: Option[T => String],
                                              spark:      SparkSession,
                                              hdfsRoot:   String,
                                              config:     Config): T => String = {

    optionalFn match {
      case Some(fn) => fn
      case None =>

        val codeVersionPrefix = if (config.hasPath("version")) {
          s"""v${config.getString("version")} - """
        } else {
          ""
        }

        val conf = spark.sparkContext.hadoopConfiguration
        val fs = org.apache.hadoop.fs.FileSystem.get(conf)
        val metadataExists = fs.exists(new org.apache.hadoop.fs.Path(compoundRunMetadataFile(hdfsRoot)))
        import spark.implicits._

        val currentRun = if (metadataExists) {
          spark.read.parquet(compoundRunMetadataFile(hdfsRoot)).select("runId").as[Int].head + 1
        } else 1

        Seq(currentRun).toDF("runId")
          .repartition(1)
          .write
          .mode("overwrite")
          .parquet(compoundRunMetadataFile(hdfsRoot))

        t: T => codeVersionPrefix + currentRun.toString
    }
  }

}