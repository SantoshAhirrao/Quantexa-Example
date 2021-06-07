package com.quantexa.example.etl.projects.fiu.hotlist

import com.quantexa.etl.address.AddressParser
import com.quantexa.etl.address.core.Model.Configuration.AddressParserConfiguration
import com.quantexa.etl.address.core.Model.Configuration.LibpostalConfiguration
import com.quantexa.etl.address.template.CompositeAddressParser
import com.quantexa.etl.address.template.global.GlobalAddressParser
import com.quantexa.etl.address.template.hk.HKAddressParser
import com.quantexa.etl.address.template.singapore.SingaporeAddressParser
import com.quantexa.etl.address.template.uk.UKAddressParser
import com.quantexa.etl.address.template.us.USAddressParser
import com.quantexa.etl.utils.PhoneParser
import com.quantexa.etl.utils.cleansing.Individual.parseName
import com.quantexa.etl.utils.cleansing.{Business, Telephone}
import com.quantexa.example.etl.projects.fiu.ETLConfig
import com.quantexa.example.etl.projects.fiu.utils.ProjectParsingUtils._
import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist
import com.quantexa.model.core.datasets.ParsedDatasets.LensParsedTelephone
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

/** *
  * QuantexaSparkScript used to parse/cleanse the hierarchical model components for the FIU Smoke Test Data
  * Input: DocumentDataModel/DocumentDataModel.parquet
  * Output: DocumentDataModel/CleansedDocumentDataModel.parquet
  *
  * Stage 4
  * At this stage we want to parse/cleanse the raw values of the hierarchical model and populate the parsed part of the model as best we can
  *
  */
object CleanseCaseClass extends TypedSparkScript[ETLConfig] {
  val name: String = "CleanseCaseClass - Hotlist"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], projectConfig: ETLConfig, etlMetricsRepository: ETLMetricsRepository): Unit = {
    if (args.nonEmpty) {
      logger.warn(args.length + " arguments were passed to the script and are being ignored")
    }

    import spark.implicits._

    val caseClassPath = projectConfig.hotlist.caseClassPath
    val destinationPath = projectConfig.hotlist.cleansedCaseClassPath

    val caseClassDS = spark.read.parquet(caseClassPath).as[Hotlist]

    val parsedDS = caseClassDS.mapPartitions(partition => {
      //Create any non-serializable parser objects
      val useStubParser = sys.props.getOrElse("stubParser", "false").toBoolean

      val addressParserConfiguration = if (!useStubParser)
        AddressParserConfiguration(Some(LibpostalConfiguration()))
      else AddressParserConfiguration(None)

      val compositeParser = new CompositeAddressParser(
        Map(
          "GBR" -> UKAddressParser,
          "USA" -> USAddressParser,
          "HKG" -> HKAddressParser,
          "SGP" -> SingaporeAddressParser),
        GlobalAddressParser)

      val logger: Logger = Logger.getLogger(this.getClass)
      partition.map(parseHotlist(_, compositeParser, addressParserConfiguration)(logger))
    })

    parsedDS.write.mode("overwrite").parquet(destinationPath)

    etlMetricsRepository.size("cleansed case class - hotlist", parsedDS.count)

    etlMetricsRepository.time("CleanseCaseClass - Hotlist", parsedDS.write.mode("overwrite").parquet(destinationPath))
  }

  def parseHotlist(hotlist: Hotlist, addressParser: AddressParser, addressConfig: AddressParserConfiguration)(logger: Logger): Hotlist = {
    val hotListWithparsedCountries = hotlist.copy(
      //Parse Hotlist Level
      parsedIndividualName = parseName(hotlist.fullName)
        .map(_.map(_.toParentVersion[Hotlist]).toSeq)
        .getOrElse(Seq.empty),
      dateOfBirthParts = parseDate(hotlist.dateOfBirth),
      parsedBusinessName = Business.parse(hotlist.registeredBusinessName)
        .map(_.toParentVersion[Hotlist]),
      parsedAddress = hotlist.consolidatedAddress.map(addressParser.parse(addressConfig, _))
        .map(parsedAddressToLensParsedAddress[Hotlist]))

    // Required to retrieve parsed Address Country
    hotListWithparsedCountries.copy(
      cleansedTelephoneNumber = hotlist.telephoneNumber.flatMap { number =>
        val fallBackNumber = Telephone.parse(Some(number)).map(_.toParentVersion[Hotlist])
        val countryWithFallBack = hotListWithparsedCountries
          .parsedAddress.map(_.country)
          .getOrElse(Some("GB"))

        PhoneParser.validate(number, countryWithFallBack).fold(
          { error =>
            //logger.info("Failed to Validate PhoneNumber, falling back to default parser.", error)
            fallBackNumber
          },
          { validatedNumber =>
            Some(
              LensParsedTelephone[Hotlist](
                telephoneDisplay = validatedNumber.internationalFormat,
                telephoneClean = validatedNumber.internationalFormat,
                // TODO Change implementation once merge request is complete
                // https://gitlab.com/Quantexa/community/incubators/quantexa-etl-incubator/merge_requests/45
                countryCode = Option(validatedNumber.getRegionCodeForNumber.getOrElse("Unknown"))
              )
            )
          }
        )
      }
    )
  }
}
