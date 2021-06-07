package com.quantexa.example.etl.projects.fiu.countrycorruption

import com.quantexa.example.etl.projects.fiu.ETLConfig
import com.quantexa.scriptrunner.util.DevelopmentConventions.FolderConventions
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, SparkScript, TypedSparkScript}
import org.apache.log4j.Logger
import io.circe.generic.auto.exportDecoder
import org.apache.spark.sql.SaveMode.Overwrite
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.IntegerType

object ImportRawToParquet extends TypedSparkScript[ETLConfig] {
  val name = "Create Country Corruption Index"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          logger: Logger,
          args: Seq[String],
          etlConfig: ETLConfig,
          etlMetricsRepository: ETLMetricsRepository): Unit = {
    import spark.implicits._

    val inputPath = etlConfig
      .scoringLookups
      .getOrElse(throw new IllegalStateException("No input path found in config. Please specify a value for 'scoringLookups.countryCorruption'."))
      .countryCorruption

    val destinationPath = etlConfig
      .scoringLookups
      .getOrElse(throw new IllegalStateException("No destination path found in config. Please specify a value for 'scoringLookups.outputRoot'."))
      .outputRoot

    val corruptionIndex = spark.sqlContext.read.
      option("useHeader", "true").
      option("treatEmptyValuesAsNulls", "false").
      option("inferSchema", "true").
      option("header", "true").
      csv(inputPath)

    corruptionIndex.select('ISO3, 'CPIScore2017.cast(IntegerType)).withColumnRenamed("CPIScore2017", "corruptionIndex").
      write.mode(Overwrite).parquet(s"$destinationPath/CorruptionIndex.parquet")
  }
}