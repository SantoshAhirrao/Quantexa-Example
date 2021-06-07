package com.quantexa.example.scoring.utils

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.util.{Failure, Success, Try}

object TypedConfigReader {

  def getValuesFromConfig(): ProjectExampleConfig = {
    val config = ConfigFactory.load().resolve()
    config.as[ProjectExampleConfig]("scoring.fiu")
  }

  case class ScoringLookup(
                            path: String,
                            format: String,
                            indexName: String,
                            documentType: String,
                            idField: String)

  case class ElasticSettings(
                              esClusterName: String,
                              esNodeIps: String,
                              esPort: String)

  /**
    *
    * @param filePath - Full path of the score parameter file (local disk or on classpath)
    * @param classpath - Whether to load from classpath or not
    */
  case class ScoreParameterFile(
                               filePath:String,
                               classpath:Boolean=false
                               )
  /**
    *
    * @param name
    * @param hdfsFolder - Root folder
    * @param hdfsFolderCustomer - Root customer folder
    * @param hdfsFolderHotlist - Root hotlist folder
    * @param hdfsFolderTransactions - Root transaction folder
    * @param hdfsFolderScoring - scoreOutputRoot
    * @param hdfsFolderCountryCorruption - Root country corruption folder
    * @param documentType
    * @param runDate - This must be of format YYYY-MM-DD
    * @param scorecardExcelFile - Path to output Scorecard Excel file
    * @param elasticSettings - Elastic Loader settings
    * @param scoreParameterFile - Score parameter location
    * @param highRiskCountryCodesLookup - High risk country codes lookup configuration
    * @param postcodePricesLookup  - Postcode prices lookup configuration
    * @param txnScoreToCustomerRollup
    * @param txnCustomerDateScoreToCustomerRollup
    */
  case class ProjectExampleConfig(
                                   name: String,
                                   hdfsFolder: String,
                                   hdfsFolderCustomer: String,
                                   hdfsFolderHotlist: String,
                                   hdfsFolderTransactions: String,
                                   hdfsFolderScoring: String,
                                   hdfsFolderCountryCorruption: String,
                                   runDate: String, //java.sql.Date is not supported as a type
                                   scorecardExcelFile: Option[String],
                                   elasticSettings: ElasticSettings,
                                   scoreParameterFile: ScoreParameterFile,
                                   highRiskCountryCodesLookup: ScoringLookup,
                                   postcodePricesLookup: ScoringLookup,
                                   txnScoreToCustomerRollup: ScoringLookup,
                                   txnCustomerDateScoreToCustomerRollup: ScoringLookup
                                 ) {
    val hdfsFolderTransactionFacts = s"$hdfsFolderScoring/Facts/TransactionFacts.parquet"
    val hdfsFolderCustomerDateFacts = s"$hdfsFolderScoring/Facts/CustomerDateFacts.parquet"
    val hdfsFolderCustomerMonthFacts = s"$hdfsFolderScoring/Facts/CustomerMonthFacts.parquet"
    val hdfsFolderCustomerMonthFactsWithNeighbourStats = s"$hdfsFolderScoring/Facts/CustomerMonthFactsWithNeighbourStats.parquet"
    val hdfsFolderCustomerKNNFacts = s"$hdfsFolderScoring/Facts/KNNCustomerMonth.parquet"
    val hdfsFolderCustomerFacts = s"$hdfsFolderScoring/Facts/CustomerFacts.parquet"
    val hdfsFolderAggregatedTransactionFacts =s"$hdfsFolderScoring/Facts/AggregatedTransactionFacts.parquet"
    val runDateDt = Try(java.sql.Date.valueOf(this.runDate)) match {
      case Success(date) => date
      case Failure(error) => throw new IllegalArgumentException(s"Invalid date provided in scoring configuration, found ${this.runDate} . Format must be YYYY-MM-DD")
    }
  }


  object ProjectExampleConfig {
    def default(hdfsFolder:String) = {
      val hdfsFolderCustomer = s"$hdfsFolder/customer"
      val hdfsFolderScoring = s"$hdfsFolder/scoring"
      val elasticSettings = ElasticSettings(
        esClusterName = "toothless",
        esNodeIps = "10.1.1.16",
        esPort = "9200")
      val riskCountryCodesLookup = ScoringLookup(
        path = s"$hdfsFolderScoring/raw/csv/highRiskCountryCodes.csv",
        format = "csv",
        indexName = "fiu-smoke",
        documentType = "riskcountrycodelookup",
        idField = "riskCountryCode")
      val postcodesPriceMapLookup = ScoringLookup(
        path = s"$hdfsFolderScoring/raw/csv/postcode-lookup.csv",
        format = "csv",
        indexName = "fiu-smoke",
        documentType = "postcodepriceslookup",
        idField = "postcode")
      val txnScoreToCustomerRollupLookup = ScoringLookup(
        path = s"",
        format = "elastic",
        indexName = "search-fiu-smoke-txnscoretocustomerrollup",
        documentType = "default",
        idField = "subject")
      val txnCustomerDateScoreToCustomerRollupLookup = ScoringLookup(
        path = s"",
        format = "elastic",
        indexName = "search-fiu-smoke-txncustomerdatescoretocustomerrollup",
        documentType = "default",
        idField = "subject")

      ProjectExampleConfig(
        name = "Default Config",
        hdfsFolder = hdfsFolder,
        hdfsFolderCustomer = hdfsFolderCustomer,
        hdfsFolderHotlist = s"$hdfsFolder/hotlist",
        hdfsFolderTransactions = s"$hdfsFolder/transaction",
        hdfsFolderScoring = hdfsFolderScoring,
        hdfsFolderCountryCorruption = hdfsFolderScoring,
        runDate = "2017-08-01",
        scorecardExcelFile = None,
        elasticSettings = elasticSettings,
        scoreParameterFile = ScoreParameterFile( "/parameterFile.csv", classpath = true),
        highRiskCountryCodesLookup = riskCountryCodesLookup,
        postcodePricesLookup = postcodesPriceMapLookup,
        txnScoreToCustomerRollup = txnScoreToCustomerRollupLookup,
        txnCustomerDateScoreToCustomerRollup = txnCustomerDateScoreToCustomerRollupLookup
      )
    }
  }
}