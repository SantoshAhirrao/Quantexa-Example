package com.quantexa.example.scoring.batch.utils.fiu

import java.nio.file.{Path, Paths}

import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist
import com.quantexa.example.model.fiu.transaction.TransactionModel.Transaction
import com.quantexa.example.scoring.batch.scores.fiu.DocumentTestData
import com.quantexa.example.scoring.model.fiu.FactsModel.{CustomerDateFacts, TransactionFacts}
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerDateScoreOutput, CustomerRollup}
import com.quantexa.scoring.framework.model.ScoreDefinition.Model.{DocumentTypeInformation, documentSource}
import org.apache.log4j.{Level, Logger}

trait ScoringTestSuite extends SparkTestScoringFrameworkTestSuite {
  override def testPath = "endtoendtest"

  lazy val logger = Logger.getLogger("quantexa-spark-script")
  logger.setLevel(Level.INFO)
  lazy val metrics = Metrics.repo


  val customer: Seq[Customer] = Seq(DocumentTestData.testCustomer)
  val hotlist: Seq[Hotlist] = Seq(DocumentTestData.testHotlist)
  val transactions: Seq[Transaction] = DocumentTestData.testTransactions
  val transactionFacts: Seq[TransactionFacts] = Seq(DocumentTestData.testTransactionFacts)


  lazy val customerPath: Path = tmp resolve Paths.get("customer", "CleansedCaseClass", "CleansedDocumentDataModel.parquet")
  lazy val hotlistPath: Path = tmp resolve Paths.get("hotlist", "CleansedCaseClass", "CleansedDocumentDataModel.parquet")
  lazy val transactionPath: Path = tmp resolve Paths.get("transactions", "CaseClass", "DocumentDataModel.parquet")
  lazy val transactionFactPath: Path = tmp resolve Paths.get("scoring", "Facts", "TransactionFacts.parquet")

  lazy val phase1DocTypes = Seq(
    DocumentTypeInformation(documentSource[CustomerDateFacts], null, null))

  lazy val phase2DocTypes = Seq(
    DocumentTypeInformation(documentSource[CustomerRollup[CustomerDateScoreOutput]], null, null))

  override def beforeAll(): Unit = {
    super.beforeAll()
    import spark.implicits._
    persistDatasets(customer.toDS -> customerPath :: Nil)()
    persistDatasets(hotlist.toDS -> hotlistPath :: Nil)()
    persistDatasets(transactions.toDS -> transactionPath :: transactionFacts.toDS -> transactionFactPath :: Nil)()
  }
}