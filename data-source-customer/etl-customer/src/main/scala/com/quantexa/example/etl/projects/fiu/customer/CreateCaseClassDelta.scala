package com.quantexa.example.etl.projects.fiu.customer

import com.quantexa.example.etl.projects.fiu.{CustomerInputFiles, DocumentConfig}
import com.quantexa.scriptrunner.util.incremental.MetaDataRepositoryImpl._
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.resolver.core.EntityGraph.DocumentId
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScriptIncremental}
import org.apache.log4j.Logger
import org.apache.spark.sql.{Dataset, Encoder, Encoders, SparkSession}
import io.circe.generic.auto._

/** *
  * QuantexaSparkScript used to calculate which data points have been updated or deleted by comparing the current case class with a previous one
  * Input: /previous/run/DocumentDataModel/DocumentDataModel.parquet, /current/run/DocumentDataModel/DocumentDataModel.parquet
  * Output: /current/run/DocumentDataModel/DocumentDataModel_Updated.parquet, /current/run/DocumentDataModel/DocumentDataModel_Deleted.parquet
  * Arguments: previous RunId
  *
  * To be run after Create Case Class (for incremental mode only)
  * The delta should be calculated as early as possible in the process.
  * A full outer join between the old and new customer case class is done
  * and duplicate records are removed if they have been updated in the
  * new dataset (equality is checked via the hascode of the caseclass).
  *
  */

object CreateCaseClassDelta extends TypedSparkScriptIncremental[DocumentConfig[CustomerInputFiles]] {

  val name = "CreateCaseClassDeltas - Customer"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: DocumentConfig[CustomerInputFiles], etlMetricsRepository: ETLMetricsRepository, metadataRunId: MetadataRunId) = {
    if (args.length > 1) logger.warn(" additional arguments were passed to the script and are being ignored.")

    import spark.implicits._
    implicit val tupleEncoder: Encoder[(String, Customer)] = Encoders.tuple(Encoders.STRING, implicitly[Encoder[Customer]])

    val metadataPath = projectConfig.metadataPath

    val previousRunId = if(args.isEmpty) {
      logger.info("No previous runId passed in, inferring from metadata.")
      inferPreviousRunId(spark, metadataPath)
    } else inferPreviousRunId(spark, metadataPath, args.head)

    logger.info(s"Previous runId found: ${previousRunId.runId}")

    val caseClassPath = projectConfig.caseClassPath
    val previousCaseClassPath = s"${projectConfig.datasourceRoot}/${previousRunId.runId}/DocumentDataModel/DocumentDataModel.parquet"

    val caseClassUpdatedPath = caseClassPath.replace("DocumentDataModel.parquet", "DocumentDataModel_Updated.parquet")
    val caseClassDeletedPath = caseClassPath.replace("DocumentDataModel.parquet", "DocumentDataModel_Deleted.parquet")

    val currentCustomerDS = spark.read.parquet(caseClassPath)
      .as[Customer]
      .map(_.copy(metaData = None))
    val oldCustomerDS = spark.read.parquet(previousCaseClassPath)
      .as[Customer]
      .map(_.copy(metaData = None))

    val joined = fullOuterJoin(oldCustomerDS, currentCustomerDS)

    val updated = getUpdatedRecords(joined, getPairDS(currentCustomerDS))
      .map(ds => ds.copy(metaData = Some(metadataRunId)))

    val deleted = getDeletedRecords(joined, getPairDS(oldCustomerDS))
      .map(d => new DocumentId("customer", d.customerIdNumberString))

    etlMetricsRepository.size("Main dataset", updated.count)
    etlMetricsRepository.time("Updated data", updated.write.mode("overwrite").parquet(caseClassUpdatedPath))
    etlMetricsRepository.size("Deleted dataset", deleted.count)
    etlMetricsRepository.time("Deleted data", deleted.write.mode("overwrite").parquet(caseClassDeletedPath))
  }

  /*
  This is used to calculate the records that have been added/updated and deleted (in conjunction with getUpdatedRecords and getDeletedRecords).
 */
  def fullOuterJoin(oldDS: Dataset[Customer], newDS: Dataset[Customer]): Dataset[(String, Option[Int], Option[Int])] = {
    import oldDS.sparkSession.implicits._
    val oldHashed = oldDS.map(x => (x.customerIdNumberString, x.hashCode)).as[(String, Int)]
    val newHashed = newDS.map(x => (x.customerIdNumberString, x.hashCode)).as[(String, Int)]

    oldHashed.join(newHashed, Seq("_1"), "outer").as[(String, Option[Int], Option[Int])]
  }

  /*
    Returns data set of all records that have been added and/or changed.
   */
  def getUpdatedRecords[T: Encoder](fullOuterJoin: Dataset[(String, Option[Int], Option[Int])],
                                    newFullCaseClass: Dataset[(String, T)])(implicit kryo: Encoder[(String, T)]): Dataset[T] = {
    import fullOuterJoin.sparkSession.implicits._

    val updatedIds = fullOuterJoin.filter(x => x match {
      case (id, None, Some(_)) => true
      case (id, Some(old), Some(current)) => old != current
      case _ => false
    }).map(_._1)

    newFullCaseClass.join(updatedIds, Seq("value"), "inner")
      .as[(String, T)]
      .map(_._2)
  }


  /*
    Returns data set of all records that have been removed.
   */
  def getDeletedRecords[T: Encoder](fullOuterJoin: Dataset[(String, Option[Int], Option[Int])],
                                    oldFullCaseClass: Dataset[(String, T)])(implicit kryo: Encoder[(String, T)]): Dataset[T] = {
    import fullOuterJoin.sparkSession.implicits._

    val deletedIDs = fullOuterJoin.filter(x => x match {
      case (id, Some(_), None) => true
      case _ => false
    }).map(_._1)

    oldFullCaseClass.join(deletedIDs, Seq("value"), "inner")
      .as[(String, T)]
      .map(_._2)
  }

  def getPairDS(ds: Dataset[Customer])(implicit kryo: Encoder[(String, Customer)]): Dataset[(String, Customer)] = {
    ds.map(x => (x.customerIdNumberString, x)).as[(String, Customer)]
  }

}
