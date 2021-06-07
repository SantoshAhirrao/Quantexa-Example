package com.quantexa.example.etl.projects.fiu.transaction

import com.quantexa.example.etl.projects.fiu.{DocumentConfig, ETLConfig, TransactionInputFile}
import com.quantexa.scriptrunner.util.incremental.MetaDataRepositoryImpl._
import com.quantexa.example.model.fiu.transaction.TransactionModel._
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript, TypedSparkScriptIncremental}
import io.circe.generic.auto._
import org.apache.log4j.Logger
import org.apache.spark.sql._

import scala.util.{Failure, Success, Try}

/***
  * QuantexaSparkScript used to parse the hierarchical model components for the transaction data
  * Input: previous/run/DocumentDataModel/AggregatedCleansedDocumentDataModel_Consolidated.parquet, current/run/DocumentDataModel/AggregatedCleansedDocumentDataModel.parquet
  * Output: /DocumentDataModel/AggregatedCleansedDocumentDataModel.parquet, /DocumentDataModel/AggregatedCleansedDocumentDataModel_Consolidated.parquet
  *
  * Stage 4.5
  * At this stage we calculate deltas between the current aggregated case class
  * and the last consolidated aggregated case class (in the case of the initial
  * run, the aggregated case class is used as the 'consolidated' case class).
  * This involves merging the aggregations between each pair of accounts from
  * the old and new dataset.
  *
  */

object CreateAggregationClassDelta extends TypedSparkScriptIncremental[DocumentConfig[TransactionInputFile]]{
  val name: String = "CreateAggregationClassDelta"

  val fileDependencies = Map.empty[String,String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: DocumentConfig[TransactionInputFile], etlMetricsRepository: ETLMetricsRepository, metadataRunId: MetadataRunId): Unit = {
    if (args.length > 1) logger.warn(" additional arguments were passed to the script and are being ignored.")

    import spark.implicits._

    val metadataPath = projectConfig.metadataPath
    val previousRunId = if(args.isEmpty) {
      logger.info("No previous runId passed in, inferring from metadata.")
      inferPreviousRunId(spark, metadataPath)
    } else inferPreviousRunId(spark, metadataPath, args.head)

    logger.info(s"Previous runId found: ${previousRunId.runId}")

    val aggregationCaseClassPath = projectConfig.aggregatedCaseClassPath
      .getOrElse("transaction.aggregatedCaseClassPath not specified in config.")
    val previousAggregationCaseClassPath =
      s"${projectConfig.datasourceRoot}/${previousRunId.runId}/DocumentDataModel/AggregatedCleansedDocumentDataModel.parquet"

    val consolidatedAggCaseClassPath =  previousAggregationCaseClassPath
      .replace("AggregatedCleansedDocumentDataModel.parquet",
        "AggregatedCleansedDocumentDataModel_Consolidated.parquet")

    val currentTransactionsBeforeUpdateDS = spark.read.parquet(aggregationCaseClassPath).as[AggregatedTransaction]
    val oldDS = Try(spark.read.parquet(consolidatedAggCaseClassPath).as[AggregatedTransaction]) match {
      case Success(txns) =>
        logger.info("Loading Consolidated data from previous run...")
        txns
      case Failure(e : AnalysisException) =>
        logger.info("Loading full transaction data from previous run...")
        spark.read.parquet(previousAggregationCaseClassPath).as[AggregatedTransaction]
      case Failure(exception) => throw exception
    }

    //TODO: Remove this hack. Here only because spark can't read from a location and then overwrite the file in that location.
    val beforeUpdatePath = aggregationCaseClassPath.replace("AggregatedCleansedDocumentDataModel.parquet",
      "AggregatedCleansedDocumentDataModel_BeforeUpdate.parquet")
    currentTransactionsBeforeUpdateDS.write.parquet(beforeUpdatePath)
    val currentDS = spark.read.parquet(beforeUpdatePath).as[AggregatedTransaction]
    // End of hack.

    // compute deltas
    val initDeltaRDD = pairDsById(currentDS, oldDS)
    val deltaDS = reduceAggregates(initDeltaRDD)

    // updated consolidated case class
    val consolidatedDS = createNewConsolidatedAggregatedCaseClass(deltaDS, oldDS)

    val consolidatedTransactionDestinationPath = aggregationCaseClassPath.replace(
      "AggregatedCleansedDocumentDataModel.parquet",
      "AggregatedCleansedDocumentDataModel_Consolidated.parquet")


    etlMetricsRepository.time("CreateAggregationClassDelta -- deltas", deltaDS.write.mode("overwrite").parquet(aggregationCaseClassPath))
    etlMetricsRepository.size("Deltas dataset", deltaDS.count())

    etlMetricsRepository.time("CreateAggregationClassDelta -- consolidated", consolidatedDS.write.mode("overwrite").parquet(consolidatedTransactionDestinationPath))
    etlMetricsRepository.size("Consolidated dataset", consolidatedDS.count())
  }

  def pairDsById(newDS: Dataset[AggregatedTransaction], oldDS: Dataset[AggregatedTransaction]): Dataset[(String, AggregatedTransaction, Option[AggregatedTransaction])] = {
    import newDS.sparkSession.implicits._

    val newStruct = newDS.map(x => (x.aggregatedTransactionId, x)).as[(String, AggregatedTransaction)]
    val oldStruct = oldDS.map(x => (x.aggregatedTransactionId, x)).as[(String, AggregatedTransaction)]

    newStruct.join(oldStruct, Seq("_1"), "left_outer").as[(String, AggregatedTransaction, Option[AggregatedTransaction])]
  }

  def reduceAggregates(ds: Dataset[(String, AggregatedTransaction, Option[AggregatedTransaction])]): Dataset[AggregatedTransaction] = {
    import ds.sparkSession.implicits._

    ds.map {
      case (id, current, Some(old)) => combineAggs(current, old)
      case (id, current, None) => current
    }.as[AggregatedTransaction]
  }

  def combineAggs(newAgg: AggregatedTransaction, oldAgg: AggregatedTransaction): AggregatedTransaction = {
    val updatedCurrencies: Seq[String] = (newAgg.currencies ++ oldAgg.currencies).distinct

    // combine stats
    val newStats = newAgg.txnStatsTotal
    val oldStats = oldAgg.txnStatsTotal

    val updatedAggStats = (newStats, oldStats) match {
      case (Some(x), None) => x
      case (None, Some(y)) => y
      case (Some(x), Some(y)) => combineStats(x, y)
      case (None, None) => throw new IllegalArgumentException("no aggregated case classes to aggregate")
    }

    oldAgg.copy(
      currencies = updatedCurrencies,
      txnStatsTotal = Some(updatedAggStats),
      lastDate = newAgg.lastDate
    )
  }

  def combineStats(newStats: AggregatedStats, oldStats: AggregatedStats): AggregatedStats = {
    val updatedTxnSum = newStats.txnSum.get + oldStats.txnSum.get
    val updatedTxnCnt = newStats.txnCount.get + oldStats.txnCount.get
    val updatedTxnMin = Math.min(newStats.txnMin.get, oldStats.txnMin.get)
    val updatedTxnMax = Math.max(newStats.txnMax.get, oldStats.txnMax.get)

    val updatedBaseSum = newStats.txnSumBase.get + oldStats.txnSumBase.get
    val updatedBaseMin = Math.min(newStats.txnMinBase.get, oldStats.txnMinBase.get)
    val updatedBaseMax = Math.max(newStats.txnMaxBase.get, oldStats.txnMaxBase.get)

    AggregatedStats(
      Some(updatedTxnSum),
      Some(updatedTxnCnt),
      Some(updatedTxnMin),
      Some(updatedTxnMax),
      Some(updatedBaseSum),
      Some(updatedBaseMin),
      Some(updatedBaseMax)
    )
  }

  def createNewConsolidatedAggregatedCaseClass(deltas: Dataset[AggregatedTransaction], oldDS: Dataset[AggregatedTransaction]): Dataset[AggregatedTransaction] = {
    import deltas.sparkSession.implicits._

    val newHashed = deltas.map(x => (x.aggregatedTransactionId, x.hashCode))
    val oldHashed = oldDS.map(x => (x.aggregatedTransactionId, x.hashCode))

    val joinedDS = oldHashed.join(newHashed, Seq("_1"), "outer").as[(String, Option[Int], Option[Int])]
    val consolidatedIds = joinedDS.filter (x => x match {
      case (id, Some(_), None) => true
      case _ => false
    }).map(_._1)
      .withColumnRenamed("value", "_1")

    val changed = deltas.map(x => (x.aggregatedTransactionId, x))
    val unchanged = oldDS.map(x => (x.aggregatedTransactionId, x))
      .join(consolidatedIds, Seq("_1"), "inner").as[(String, AggregatedTransaction)]

    unchanged.union(changed)
      .map(_._2)
      .as[AggregatedTransaction]
  }

}
