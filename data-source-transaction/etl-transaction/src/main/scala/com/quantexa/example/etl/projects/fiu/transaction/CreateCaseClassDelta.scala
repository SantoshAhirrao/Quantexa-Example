package com.quantexa.example.etl.projects.fiu.transaction

import java.sql.Date

import com.quantexa.example.etl.projects.fiu.{DocumentConfig, ETLConfig, TransactionInputFile}
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScriptIncremental}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import com.quantexa.example.model.fiu.transaction.TransactionModel._
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import org.apache.spark.sql.functions._
import com.quantexa.scriptrunner.util.incremental.MetaDataRepositoryImpl._

/***
  * QuantexaSparkScript used to calculate which data points have been updated or deleted by comparing the current case class with a previous one
  * Input: /previous/run/DocumentDataModel/DocumentDataModel.parquet, /current/run/DocumentDataModel/DocumentDataModel.parquet
  * Output: /current/run/DocumentDataModel_Updated.parquet
  * Arguments: previous RunId
  *
  * Stage 2.5 to be run after Create Case Class (for incremental mode only)
  * The delta should be calculated as early as possible in the process.
  * The latest ingested posting date is used to filter out old transaction
  * case classes.
  *
  */

object CreateCaseClassDelta extends TypedSparkScriptIncremental[DocumentConfig[TransactionInputFile]]{

  val name = "CreateCaseClassDeltas - Transaction"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: DocumentConfig[TransactionInputFile], etlMetricsRepository: ETLMetricsRepository, metadataRunId: MetadataRunId) = {
    if (args.length > 1) logger.warn("additional arguments were passed to the script and are being ignored.")

    import spark.implicits._

    val metadataPath = projectConfig.metadataPath
    val previousRunId = if(args.isEmpty) {
      logger.info("No previous runId passed in, inferring from metadata.")
      inferPreviousRunId(spark, metadataPath)
    } else inferPreviousRunId(spark, metadataPath, args.head)

    logger.info(s"Previous runId found: ${previousRunId.runId}")

    val caseClassPath = projectConfig.caseClassPath
    val previousCaseClassPath = s"${projectConfig.datasourceRoot}/${previousRunId.runId}/DocumentDataModel/DocumentDataModel.parquet"
    val caseClassUpdatedPath = caseClassPath.replace("DocumentDataModel.parquet", "DocumentDataModel_Updated.parquet")

    val lastIngestedDate = getLastTransactionDateFromMetadata(spark, projectConfig.metadataPath, previousRunId)

    val transactionDS = spark.read.parquet(caseClassPath).as[Transaction]

    // Calculates deltas by filtering using lastRunDate. If lastRunDate = None, then filters by latestPostingDate from old caseclass
    val lastTransactionDate = lastIngestedDate.getOrElse({
      new Date(spark.read.parquet(previousCaseClassPath)
        .as[Transaction]
        .map(_.postingDate.getTime)
        .sort(desc("value"))
        .head)
    })

    logger.info(s"Last transaction date: $lastTransactionDate")

    val caseClassDeltas = transactionDS
      .filter(_.postingDate.after(lastTransactionDate))
      .map(transaction => transaction.copy(metaData = Some(metadataRunId)))

    etlMetricsRepository.size("CreateCaseClassDeltas", caseClassDeltas.count)
    etlMetricsRepository.time("CreateCaseClassDeltas", caseClassDeltas.write.mode("overwrite").parquet(caseClassUpdatedPath))
  }
}