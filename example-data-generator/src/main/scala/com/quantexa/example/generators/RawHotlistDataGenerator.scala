package com.quantexa.example.generators

import com.quantexa.example.generators.arbitrary.hotlist.HotlistRawGenerator
import com.quantexa.example.generators.config.GeneratorConfig
import com.quantexa.generators.utils.GeneratedEntities
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.auto._
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import com.quantexa.generators.utils.GeneratorUtils.generateCommonEntities
import com.quantexa.etl.utils.spark.SparkUtilityFunctions.writeToSingleCSV

object RawHotlistDataGenerator extends TypedSparkScript[GeneratorConfig] {

  val name = "RawHotlistDataGenerator"

  val scriptDependencies = Set.empty[QuantexaSparkScript]
  val fileDependencies = Map.empty[String, String]

  protected def run(spark: SparkSession, logger: Logger, args: Seq[String], config: GeneratorConfig,
                    etlMetricsRepository: ETLMetricsRepository) = {

    val specificEntities = generateCommonEntities(200,200,200,200,200, 21)

    generateAndWriteHotlistRecords(spark, config, specificEntities)
  }

  def generateAndWriteHotlistRecords(spark:SparkSession, config: GeneratorConfig, generatedEntities: GeneratedEntities): Unit = {
    import spark.implicits._
    import com.quantexa.example.generators.config.GeneratorLoaderImplicits._

    val hotlistRawGenerator = new HotlistRawGenerator(config, generatedEntities)

    val hotlistDataset = hotlistRawGenerator.generateRecords.toDS

    writeToSingleCSV(hotlistDataset, config.rootHDFSPath + "hotlist/raw/csv/hotlist.csv")
  }
}
