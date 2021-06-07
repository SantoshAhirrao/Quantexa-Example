package com.quantexa.example.etl.projects.fiu.transaction

import com.quantexa.etl.elastic.ElasticLoadScriptIncremental
import com.quantexa.etl.core.elastic.Elastic6LoaderCombined
import com.quantexa.example.model.fiu.transaction.TransactionModel.AggregatedTransaction
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.typesafe.config.Config
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import com.quantexa.scriptrunner.util.incremental.MetaDataRepositoryImpl.markRunComplete

/** *
  * Used to load the compounds / search documents into ElasticSearch
  * Input: Compounds/DocumentIndexInput.parquet
  *
  * Stage 6
  * We use the Quantexa Elastic Loader to load the the compounds and search documents into ElasticSearch
  *
  */
object LoadElastic extends ElasticLoadScriptIncremental[AggregatedTransaction] (documentType = "transaction", loader = new Elastic6LoaderCombined()){
  override def run(spark: SparkSession, logger: Logger, args: Seq[String], config: Config, metrics: ETLMetricsRepository, metadataRunId: MetadataRunId): Unit = {

    super.run(spark, logger, args, config, metrics, metadataRunId)

    //Mark the ETL run as complete
    markRunComplete(spark, config.getString("metadataPath"), metadataRunId)
  }
}