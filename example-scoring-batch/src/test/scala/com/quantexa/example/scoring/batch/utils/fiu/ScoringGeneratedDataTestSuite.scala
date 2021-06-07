package com.quantexa.example.scoring.batch.utils.fiu

import java.nio.file.{Path, Paths}

import org.apache.log4j.{Level, Logger}
import monocle.Lens
import monocle.macros.GenLens
import com.quantexa.example.model.fiu.customer.CustomerModel
import com.quantexa.example.model.fiu.customer.CustomerModel.Account
import com.quantexa.example.scoring.batch.generators._
import com.quantexa.example.scoring.model.fiu.FactsModel.CustomerDateFacts
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerDateScoreOutput, CustomerRollup}
import com.quantexa.example.scoring.utils.TypedConfigReader.{ProjectExampleConfig, ScoringLookup}
import com.quantexa.scoring.framework.model.ScoreDefinition.Model.{DocumentTypeInformation, documentSource}
import com.quantexa.scoring.framework.spark.execution.EngSparkData
import com.quantexa.scriptrunner.util.DevelopmentConventions.FolderConventions._
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import org.apache.spark.sql.Dataset

trait ScoringGeneratedDataTestSuite extends SparkTestScoringFrameworkTestSuite {

  lazy val logger: Logger = {
    val logger_ = Logger.getLogger("quantexa-spark-script")
    logger_.setLevel(Level.INFO)
    logger_
  }

  lazy val metrics: ETLMetricsRepository = Metrics.repo

  def csvToParquet(lkup:ScoringLookup) = lkup.copy(path=lkup.path.replaceAllLiterally("csv","parquet"),format="parquet")

  private val highRiskCountryCodesLookup: Lens[ProjectExampleConfig,ScoringLookup] = GenLens[ProjectExampleConfig](_.highRiskCountryCodesLookup)
  private val postcodePricesLookup: Lens[ProjectExampleConfig,ScoringLookup] = GenLens[ProjectExampleConfig](_.postcodePricesLookup)

  private val highRiskCountryCsvToParquet = highRiskCountryCodesLookup.modify(csvToParquet)
  private val pcdePricesCsvToParquet = postcodePricesLookup.modify(csvToParquet)

  lazy val testConfig = (highRiskCountryCsvToParquet andThen pcdePricesCsvToParquet)(ProjectExampleConfig.default(tmp))


  val numberOfTransactionsBetweenTheSameCustomer = 10
  val numberOfRecords = 100

  implicit def stringToPath(s:String):Path = Paths.get(s)
  implicit def pathToString(p:Path):String = p.toString

  lazy val corruptionIndexPath : Path = Paths.get(s"${testConfig.hdfsFolderCountryCorruption}/raw/parquet/CorruptionIndex.parquet")
  lazy val postcodePricePath : Path = Paths.get(testConfig.postcodePricesLookup.path)
  lazy val highRiskCountryCodePath : Path = Paths.get(testConfig.highRiskCountryCodesLookup.path)
  lazy val accToCusRawPath: Path = Paths.get(s"${testConfig.hdfsFolderCustomer}/raw/parquet/acc_to_cus.parquet")
  lazy val hotlistPath: Path =  cleansedCaseClassFile(testConfig.hdfsFolderHotlist)
  lazy val transactionCaseClassPath: Path = caseClassFile(testConfig.hdfsFolderTransactions)
  lazy val individualCaseClassPath: Path = Paths.get(EngSparkData.entityAttributesDSPath("individual"))
  lazy val businessCaseClassPath: Path = Paths.get(EngSparkData.entityAttributesDSPath("business"))
  lazy val transactionCleansedCaseClassPath: Path = cleansedCaseClassFile(testConfig.hdfsFolderTransactions)
  lazy val customerPath: Path = cleansedCaseClassFile(testConfig.hdfsFolderCustomer)
  lazy val scoringFrameworkRoot = testConfig.hdfsFolderScoring
  lazy val customerFactsPath: Path = testConfig.hdfsFolderCustomerFacts
  lazy val customerDateFactsPath: Path = testConfig.hdfsFolderCustomerDateFacts
  lazy val transactionFactPath: Path = testConfig.hdfsFolderTransactionFacts

  lazy val phase1DocTypes = Seq(
    DocumentTypeInformation(documentSource[CustomerDateFacts], null, null))

  lazy val phase2DocTypes = Seq(
    DocumentTypeInformation(documentSource[CustomerRollup[CustomerDateScoreOutput]], null, null))
  import spark.implicits._
  override def beforeAll(): Unit = {
    super.beforeAll()
    System.setProperty("engSparkRoot", testConfig.hdfsFolder + "/eng/")

    val postCodePriceDS = PostcodePriceGenerator.generateRecords(numberOfRecords)
    val highRiskCountryCodeDS = HighRiskCountryCodeGenerator.generateRecords(numberOfRecords)
    val customerDS: Dataset[CustomerModel.Customer] = CustomerGenerator.generateRecords(numberOfRecords)
    customerDS.cache //Ensure consistency of accounts output
    val accounts: Seq[Account] = customerDS.flatMap(_.accounts).collect.sortBy(_.primaryAccountNumber)
    if (accounts.size < 2) throw new IllegalStateException("Fewer than two accounts were generated for the customer data.")
    val corruptionIndexDS = CorruptionIndexRandomGenerator.generateRecords(numberOfRecords)
    val transactionsAccountsGenerator = TransactionAccountGenerator(accounts)
    val txnGen = TransactionsGenerator(transactionsAccountsGenerator)
    val transactionsBetweenTheSameCustomer = txnGen.transactionsBetweenTheSameCustomer(
      customerDS.orderBy($"customerIdNumber").take(numberOfTransactionsBetweenTheSameCustomer).toSeq).toDS
    val guaranteedTxns = txnGen.guaranteedTransactions(accounts.head, accounts(1)).toDS
    val transactionsDS = txnGen.generateRecords(numberOfRecords) union guaranteedTxns union transactionsBetweenTheSameCustomer
    val hotlistDS = HotlistGenerator.generateRecords(numberOfRecords)
    val individualDS = IndividualGenerator.generateRecords(numberOfRecords)
    val businessDS = BusinessGenerator.generateRecords(numberOfRecords)
    val accToCusRawDS = AccToCusRawGenerator(accounts).generateRecords(numberOfRecords)

    logger.info(s"Writing Hotlistfile File to $hotlistPath")
    persistDatasets(hotlistDS -> hotlistPath :: Nil)()
    logger.info(s"Writing Corruption File to $corruptionIndexPath")
    persistDatasets(corruptionIndexDS -> corruptionIndexPath :: Nil)()
    logger.info(s"Writing PostcodePrice File to $postcodePricePath")
    persistDatasets(postCodePriceDS -> postcodePricePath :: Nil)()
    logger.info(s"Writing HighRiskCountryCode File to $highRiskCountryCodePath")
    persistDatasets(highRiskCountryCodeDS -> highRiskCountryCodePath :: Nil)()
    logger.info(s"Writing AccToCusRaw File to $accToCusRawPath")
    persistDatasets(accToCusRawDS -> accToCusRawPath :: Nil)()
    logger.info(s"Writing transaction case class file to $transactionCaseClassPath")
    persistDatasets(transactionsDS -> transactionCaseClassPath :: Nil)()
    logger.info(s"Writing transaction cleansed case class file to $transactionCleansedCaseClassPath ")
    persistDatasets(transactionsDS -> transactionCleansedCaseClassPath :: Nil)()
    logger.info(s"Writing customer cleansed case class facts file to $customerPath")
    persistDatasets(customerDS -> customerPath :: Nil)()
    logger.warn(s"Writing individual case class to $individualCaseClassPath" )
    persistDatasets(individualDS -> individualCaseClassPath :: Nil)()
    logger.warn(s"Writing business case class to $businessCaseClassPath" )
    persistDatasets(businessDS -> businessCaseClassPath :: Nil)()
    customerDS.unpersist
  }

  override def afterAll():Unit = {
    System.clearProperty("engSparkRoot")
  }
}