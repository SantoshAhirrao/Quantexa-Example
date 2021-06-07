package com.quantexa.example.etl.projects.fiu.hotlist

import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import com.quantexa.etl.utils.cleansing.Business.isBusiness
import com.quantexa.example.etl.projects.fiu.utils.ProjectETLUtils.{valuesToNull, timestampToDate}
import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist
import com.quantexa.example.model.fiu.hotlist.HotlistRawModel.HotlistRaw
import com.quantexa.scriptrunner.QuantexaSparkScript
import com.quantexa.scriptrunner.TypedSparkScript
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import io.circe.generic.auto.exportDecoder
import com.quantexa.example.etl.projects.fiu.ETLConfig

/***
  * QuantexaSparkScript used to transform the raw Parquet files and create the hierarchical model for the FIU Smoke Test Data
  * Input: raw/parquet/hotlist.parquet
  * Output: DocumentDataModel/DocumentDataModel.parquet
  *
  * Stage 3 (NOTE: stage 2 is model creation within the models part of the project!)
  * At this stage we want to remove the nullable types from our dataset such as unwanted empty strings. We also
  * want to enforce types by converting the raw types for example a string of date format "yyyy/MM/dd" to a
  * java.sql.Date.
  * We may also want to remove any miscellaneous artifacts such as rows with null non-nullable values.
  * Once cleansed we want to construct the hierarchical complex model through the use of joins.
  *
  */
object CreateCaseClass extends TypedSparkScript[ETLConfig] {
  val name = "CreateCaseClass - Hotlist"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: ETLConfig, etlMetricsRepository: ETLMetricsRepository): Unit = {
    if (args.nonEmpty) {
      logger.warn(args.length + " arguments were passed to the script and are being ignored")
    }

    import spark.implicits._

    val hotlistPath = projectConfig.hotlist.parquetRoot + "/hotlist.parquet"
    val destinationPath = projectConfig.hotlist.caseClassPath

    val hotlistRawDS = spark.read.parquet(hotlistPath).as[HotlistRaw]

    etlMetricsRepository.size("hotlistRawDS", hotlistRawDS.count)

    //TODO: This should be handled properly with validation
    val hotlistValidatedDS = hotlistRawDS.filter(_.hotlist_id.isDefined)
      .map(rawHotlist =>
      Hotlist(
        hotlistId = rawHotlist.hotlist_id.get,
        dateAdded = rawHotlist.date_added.map(timestampToDate),
        registeredBusinessName = if(isBusiness(rawHotlist.name)) valuesToNull(rawHotlist.name.getOrElse(""), "") else None,
        parsedBusinessName = None,
        //TODO: try to replace with a Tagged Type so that only "B" or "P" values are allowed.
        personalBusinessFlag = Some(if(isBusiness(rawHotlist.name)) "B" else "P"),
        dateOfBirth = rawHotlist.dob.map(timestampToDate),
        dateOfBirthParts = None,
        fullName = if(!isBusiness(rawHotlist.name)) valuesToNull(rawHotlist.name.getOrElse(""), "") else None,
        parsedIndividualName = Seq(),
        consolidatedAddress = rawHotlist.address,
        parsedAddress = None,
        telephoneNumber = valuesToNull(rawHotlist.telephone.getOrElse(""), ""),
        cleansedTelephoneNumber = None
      ))

    etlMetricsRepository.size("case class - hotlist", hotlistValidatedDS.count)

    etlMetricsRepository.time("CreateCaseClass - Hotlist", hotlistValidatedDS.write.mode("overwrite").parquet(destinationPath))
  }
}
