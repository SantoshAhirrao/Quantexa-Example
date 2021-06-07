package com.quantexa.example.etl.projects.fiu.hotlist

import com.quantexa.example.etl.projects.fiu.hotlist.HotlistTestData.hotlistData
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils.outputDatasetToCSV
import com.quantexa.example.etl.projects.fiu._
import com.quantexa.example.model.fiu.hotlist.HotlistRawModel.HotlistRaw
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

object HotlistTestUtils {

  def extractTransform(spark: SparkSession, logger: Logger, config: ETLConfig, metrics: ETLMetricsRepository, dataRootDir: String): Unit = {
    val typesafeConfig = ConfigFactory
      .empty()
      .withValue("dataRoot", ConfigValueFactory.fromAnyRef(s"$dataRootDir/hotlist"))

    ImportRawToParquet.run(spark, logger, args = Seq(), config, metrics)
    CreateCaseClass.run(spark, logger, args = Seq(), config, metrics)
    CleanseCaseClass.run(spark, logger, args = Seq(), config, metrics)
    CreateCompounds.run(spark, logger, args = Seq(), typesafeConfig, metrics)
  }

  def writeHotlistFilesCSV(dataRootDir: String): Unit ={
    outputDatasetToCSV[HotlistRaw](hotlistData, dataRootDir + "/hotlist/raw/csv/hotlist.csv")
  }
}