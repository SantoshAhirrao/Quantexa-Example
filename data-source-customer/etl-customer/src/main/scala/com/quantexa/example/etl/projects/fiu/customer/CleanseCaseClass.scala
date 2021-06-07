package com.quantexa.example.etl.projects.fiu.customer

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
import com.quantexa.example.etl.projects.fiu.{CustomerInputFiles, DocumentConfig, ETLConfig}
import com.quantexa.example.model.fiu.customer.CustomerModel.{Account, Customer}
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScriptIncremental}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import io.circe.generic.auto.exportDecoder
import io.circe._
import io.circe.generic.semiauto._
import org.apache.log4j.Logger
import org.apache.spark.sql.{AnalysisException, SparkSession}
import com.quantexa.example.etl.projects.fiu.utils.ProjectParsingUtils._
import com.quantexa.model.core.datasets.ParsedDatasets.LensParsedTelephone
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId

import scala.util.{Failure, Success, Try}


/** *
  * QuantexaSparkScript used to parse/cleanse the hierarchical model components for the FIU Smoke Test Data
  * Input: DocumentDataModel/DocumentDataModel.parquet (or DocumentDataModel/DocumentDataModel_Updated.parquet after initial run)
  * Output: DocumentDataModel/CleansedDocumentDataModel.parquet
  *
  * At this stage we want to parse/cleanse the raw values of the hierarchical model and populate the parsed part of the model as best we can
  *
  */
object CleanseCaseClass extends TypedSparkScriptIncremental[DocumentConfig[CustomerInputFiles]] {
  val name: String = "CleanseCaseClass - Customer"

  val fileDependencies = Map.empty[String, String]

  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          logger: Logger,
          args: Seq[String],
          projectConfig: DocumentConfig[CustomerInputFiles],
          etlMetricsRepository: ETLMetricsRepository,
          metadataRunId: MetadataRunId): Unit = {

    if (args.nonEmpty) {
      logger.warn(args.length + " arguments were passed to the script and are being ignored")
    }

    import spark.implicits._

    val caseClassPath = projectConfig.caseClassPath
    val caseClassUpdatedPath = caseClassPath.replace("DocumentDataModel.parquet", "DocumentDataModel_Updated.parquet")

    val destinationPath = projectConfig.cleansedCaseClassPath

    val customerDS = Try(spark.read.parquet(caseClassUpdatedPath).as[Customer]) match {
      case Success(custDS) =>
        logger.info("Loading and cleansing only changes since last run...")
        custDS
      case Failure(e: AnalysisException) =>
        logger.info("Loading and cleansing full dataset...")
        spark.read.parquet(caseClassPath).as[Customer]
      case Failure(exception) =>
        logger.error(s"Failed to load customer case class data. Paths checked: \n $caseClassPath \n $caseClassUpdatedPath")
        throw exception
    }

    etlMetricsRepository.size("case class", customerDS.count)

    val parsedDS = customerDS.mapPartitions(partition => {
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

      partition.map(customer => {
        val customerWithMetadata = customer.copy(metaData = Some(metadataRunId))
        val logger: Logger = Logger.getLogger(this.getClass)
        parseCustomer(customerWithMetadata, compositeParser, addressParserConfiguration)(logger)
      })
    })

    etlMetricsRepository.size("cleansed case class", parsedDS.count)
    etlMetricsRepository.time("CleanseCaseClass", parsedDS.write.mode("overwrite").parquet(destinationPath))
  }

  def parseCustomer(customer: Customer, addressParser: AddressParser, addressParserConfig: AddressParserConfiguration)(logger: Logger): Customer = {
    customer.copy(
      //Parse Customer Level
      parsedCustomerName = parseName(customer.fullName).map(_.map(_.toParentVersion[Customer]).toSeq).getOrElse(Seq.empty),
      dateOfBirthParts = parseDate(customer.dateOfBirth),
      parsedBusinessName = Business.parse(customer.registeredBusinessName).map(_.toParentVersion[Customer]),
      parsedAddress = customer.consolidatedCustomerAddress.map(addressParser.parse(addressParserConfig, _))
        .map(parsedAddressToLensParsedAddress[Customer]),
      accounts = parseAccounts(customer.accounts, addressParser, addressParserConfig)(logger)
    )
  }

  def parseAccounts(accounts: Seq[Account], addressParser: AddressParser, addressParserConfig: AddressParserConfiguration)(logger: Logger): Seq[Account] = {
    accounts.map(account => {
      account.copy(
        //Parse Account Level
        cleansedTelephoneNumber = account.telephoneNumber.flatMap { number =>
          val fallBackNumber = Telephone.parse(Some(number)).map(_.toParentVersion[Account])
          val countryWithFallBack = account.country.orElse(Some("GB"))

          PhoneParser.validate(number, countryWithFallBack).fold(
            { error =>
              //logger.info("Failed to Validate PhoneNumber, falling back to default parser.", error)
              fallBackNumber
            },
            { validatedNumber =>
              Some(
                LensParsedTelephone[Account](
                  telephoneDisplay = validatedNumber.internationalFormat,
                  telephoneClean = validatedNumber.internationalFormat,
                  // TODO Change implementation once merge request is complete
                  // https://gitlab.com/Quantexa/community/incubators/quantexa-etl-incubator/merge_requests/45
                  countryCode = Option(validatedNumber.getRegionCodeForNumber.getOrElse("Unknown"))
                )
              )
            }
          )
        },
        parsedAddress = account.consolidatedAccountAddress.map(addressParser.parse(addressParserConfig, _))
          .map(parsedAddressToLensParsedAddress[Account]))
    })
  }
}
