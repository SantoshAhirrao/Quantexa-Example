package com.quantexa.example.scoring.batch.utils.elastic

import com.quantexa.elastic.client.QElasticClient
import com.quantexa.elastic.loader.utils.settings.ElasticDataModelSettings
import com.quantexa.etl.core.elastic.ElasticLoaderResultModels._
import com.quantexa.etl.core.elastic._
import com.quantexa.etl.core.utils.metrics.{JobMetrics, JobMetricsNoOp}
import org.apache.log4j.Logger

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.concurrent.ExecutionContext.Implicits.global
import com.quantexa.etl.core.utils.elastic.{IndexDefinitionUtils, IndexSettingsUtils, PostLoadUtils, ShardUtils}
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


case class GenericEs6Loader(clientProvider: (ElasticLoaderSettings, String) => QElasticClient = Client.buildClient) extends GenericElasticLoaderCombined {

  override def load[T <: Product : TypeTag](spark: SparkSession,
                                            logger: Logger,
                                            documentType: String,
                                            documentPath: String,
                                            settings: ElasticLoaderSettings,
                                            mappingSettings: ElasticDataModelSettings[T],
                                            documentIdField: String,
                                            metrics: JobMetrics = JobMetricsNoOp): Unit = {

    import spark.implicits._
    implicit val metricsRepo: JobMetrics = metrics
    val scheduler = monix.execution.Scheduler.Implicits.global

    settings.addFilesToContext(spark.sparkContext)

    val elasticClient = clientProvider(settings, "search")

    try {

      def waitForIndexId(client: QElasticClient, index: String) = {
        Await.result(IndexSettingsUtils.getIndexUuid(client, index).runToFuture(scheduler), FiniteDuration(30, "seconds"))
      }

      val loadSearch = true

      val indexTypes: Seq[IndexDetailsContainer] = Seq(IndexDetailsContainer(elasticClient, settings.searchIndexName, "search", None, loadSearch))


      val inputDf = spark.read.load(documentPath)
      val totalDocs = inputDf.count

      logger.info(s"Total docs to load: $totalDocs")

      settings match {
        case full: ElasticLoaderFullSettings =>
          logger.info("Running loader full mode")
          runLoad(full)
        case _ => throw new IllegalArgumentException("Attempting to run in non full mode")
      }

      def runLoad(settings: ElasticLoaderSettings): Unit = {

        metrics.size("Complete Index Input", totalDocs)

        val encoder: Encoder[T] = org.apache.spark.sql.Encoders.product[T]

        val indexInputDS = inputDf.as[T](encoder)

        // Not expecting to have multiple values for shards in here, and the lookups are likely to be very low weight, so setting a default value as 1
        val shards = settings.index.shards.getOrElse(Map.empty).values.toSeq.headOption.getOrElse(1)

        val indexInfo = indexTypes.map { details =>
          IndexDefinitionUtils.createOrUpdateIndexDefinitions(
            details.client,
            details.indexName,
            details.indexType,
            details.resolverSubType,
            settings,
            mappingSettings,
            shards,
            scheduler)
          val indexUuid = waitForIndexId(details.client, details.indexName)
          IndexSettingsUtils.setIndicesBulkSettingsV6(details.client, details.indexName, settings, settings.indexAdmin.bulkLoadSettings, scheduler)
          IndexInfo(details.indexName, indexUuid, Some(shards.toLong))
        }

        val sparkLoaderConfigForSearch = Map("es.batch.write.refresh" -> "false") ++ settings.sparkLoaderSettings("search")

        val sparkCtx = spark.sqlContext.sparkContext
        val sparkCfg = new SparkSettingsManager().load(sparkCtx.getConf)
        val esCfgForSearch = new PropertiesSettings().load(sparkCfg.save()).merge(sparkLoaderConfigForSearch.asJava)

        InitializationUtils.checkIdForOperation(esCfgForSearch)
        InitializationUtils.checkIndexExistence(esCfgForSearch)

        val writer = new IndexWriter[T](esCfgForSearch.save(), settings, clientProvider, documentIdField)

        val files = indexInputDS.rdd.partitions.map { part =>
          (part.index, part.asInstanceOf[FilePartition].files.map(_.filePath))
        }.toMap

        val attempt = 1L
        val numberOfStartingFiles = files.size

        val results = indexInputDS.rdd.mapPartitionsWithIndex { case (label, input) =>
          val (isSuccess, message) = Try(writer.writeTask(TaskContext.get(), input)) match {
            case Success(_) => (true, "Partition contents successfully written to elastic")
            case Failure(err) => (false, err.getMessage)
          }
          Iterator(PartitionResultRow(files(label), isSuccess, message))
        }.flatMap { prr =>
          prr.fileNames.map(FileResultRow(_, s"$attempt-${indexInfo.map(_.uuid).sorted.mkString("-")}", prr.wasSuccessful, prr.message))
        }.groupBy(_.fileName).map(_._2.toSeq.reduceLeft(_ + _)).toDS()

        val numberOfSuccessfullyCompletedFiles = results.filter(_.wasSuccessful).count()

        //TODO: Write out results somewhere.
        //If the look up is large and takes a while to load to elastic, we might want to the ability to identify failing
        //partitions (if any) to avoid reloading the entire thing.

        indexTypes.foreach { details => PostLoadUtils.executePostLoadActionsV6(settings, details.indexName, details.client, scheduler) }

        val numberOfFailedFiles = numberOfStartingFiles - numberOfSuccessfullyCompletedFiles.toInt
        if(numberOfFailedFiles > 0) throw LoadingFailureException(s"$numberOfFailedFiles of $numberOfStartingFiles did not load successfully, check metadata at PATH for details.")
      }
    } finally {
      elasticClient.close()
    }

  }

}
