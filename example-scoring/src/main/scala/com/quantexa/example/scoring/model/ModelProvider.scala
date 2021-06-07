package com.quantexa.example.scoring.model

import com.quantexa.scoring.framework.model.scoringmodel.ScoringModelProvider
import com.quantexa.example.scoring.model.fiu.{FiuModelDefinition => Fiu}
import com.quantexa.example.scoring.utils.TypedConfigReader._

//FIXME: IP-518 This is a temporary fix to make sure app can start up
class RealtimeModelProvider  extends ScoringModelProvider {
  private val hdfsFold = "/user/quantexa/smoketest"
  private val hdfsFoldScoring = s"$hdfsFold/scoring"
  val config:ProjectExampleConfig = ProjectExampleConfig(
    name = "Example config",
    hdfsFolder = hdfsFold,
    hdfsFolderCustomer = s"$hdfsFold/customer",
    hdfsFolderHotlist = s"$hdfsFold/hotlist",
    hdfsFolderTransactions = s"$hdfsFold/transactions",
    hdfsFolderScoring = s"$hdfsFold/scoring",
    hdfsFolderCountryCorruption = s"$hdfsFold/countryCorruption",
    runDate = "2018-12-25",
    scorecardExcelFile = None,
    elasticSettings = ElasticSettings(esClusterName= "toothless",
      esNodeIps= "10.1.1.16",
      esPort= "9200"),
    scoreParameterFile =  ScoreParameterFile("./parameterFile.csv"),
    highRiskCountryCodesLookup = ScoringLookup(path = s"$hdfsFold/customer/raw/parquet/riskCountryCodes.parquet", format = "parquet",
      indexName = "fiu-smoke", documentType = "riskcountrycodelookup", idField = "riskCountryCode"),
    postcodePricesLookup = ScoringLookup(path = s"$hdfsFold/customer/raw/parquet/postcode-lookup.parquet", format = "parquet",
      indexName = "fiu-smoke", documentType = "postcodepriceslookup", idField = "postcode"),
    txnScoreToCustomerRollup = ScoringLookup(path = s"", format = "csv", indexName = "search-ci-test-txnscoretocustomerrollup", documentType = "default", idField = "subject"),
    txnCustomerDateScoreToCustomerRollup  = ScoringLookup(path = s"", format = "csv", indexName = "search-ci-test-txncustomerdatescoretocustomerrollup", documentType = "default", idField = "subject")
  )
  def scoringModels = Seq(Fiu(config))
}