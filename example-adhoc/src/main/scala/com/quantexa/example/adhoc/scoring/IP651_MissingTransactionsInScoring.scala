package com.quantexa.example.adhoc.scoring

import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.scoring.batch.model.fiu.TransactionInputCustomScore
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.sql.functions._

/**
  * Background: 1,999 transactions were in the subset (cleansed), however only 55 being read by scoring
  *             Due to an issue in how transactions were generated AND only reading transactions where customer is originator
  *             As part of the fix, the transaction data was re-generated to higher volumes.
  * JIRA link: https://quantexa.atlassian.net/browse/IP-651
  */
object IP651_MissingTransactionsInScoring {
  val spark:SparkSession = ???
  import spark.implicits._

  val hdfsFolderRoot = "/user/nicksutcliffe/project-example/"
  val txn = spark.read.parquet(s"$hdfsFolderRoot/transaction/DocumentDataModel/CleansedDocumentDataModel.parquet")
  txn.count
  //21315

  val customer = spark.read.parquet(s"$hdfsFolderRoot/customer/DocumentDataModel/CleansedDocumentDataModel.parquet")

  val customerToAccount = customer.select($"customerIdNumber", explode($"accounts")).select("customerIdNumber","col.primaryAccountNumber")

  val origAccounts = txn.select("transactionId","originatingAccount.accountId", "debitCredit")
  val beneAccounts = txn.select("transactionId","beneficiaryAccount.accountId", "debitCredit")

  val checkOrigAccounts = origAccounts.join(customerToAccount, customerToAccount("primaryAccountNumber") === origAccounts("accountId"))

  val checkBeneAccounts = beneAccounts.join(customerToAccount, customerToAccount("primaryAccountNumber") === beneAccounts("accountId"))

  checkOrigAccounts.count
  //13363

  checkBeneAccounts.count
  //12300

  checkOrigAccounts.count + checkBeneAccounts.count
  //25663 expected output transactions on transaction Facts

  object TestTICS extends TransactionInputCustomScore {
    val id: String = "TestTICS"
    val config: ProjectExampleConfig = ProjectExampleConfig.default(hdfsFolderRoot)

    def processScoreableTransaction(spark: SparkSession, transactions: Dataset[ScoreableTransaction])(implicit scoreInput: ScoreModel.ScoreInput): Dataset[ScoreableTransaction] = {
      transactions
    }

  }

  val fixedDs = TestTICS.score(spark)(ScoreInput.empty).asInstanceOf[Dataset[ScoreableTransaction]]

  fixedDs.count
  //25663 GOOD this is what I was looking for

  fixedDs.groupBy("customerOriginatorOrBeneficiary").count.show
  //+-------------------------------+-----+
  //|customerOriginatorOrBeneficiary|count|
  //+-------------------------------+-----+
  //|                     originator|13278| //Expected 13363
  //|                    beneficiary|12385| //Expected 12300
  //+-------------------------------+-----+

  //OK There is some discrepency from what I expected. Let's investigate:

  checkOrigAccounts.groupBy("debitCredit").count.show
  //+-----------+-----+
  //|debitCredit|count|
  //+-----------+-----+
  //|          D|12289|
  //|          C| 1074|
  //+-----------+-----+

  checkBeneAccounts.groupBy("debitCredit").count.show

  //+-----------+-----+
  //|debitCredit|count|
  //+-----------+-----+
  //|          D|11311|
  //|          C|  989|
  //+-----------+-----+

  println(s"Number of Originators = OrigAccountsDebits + BeneAccountCredits = ${12289+989}")
  //Number of Originators = OrigAccountsDebits + BeneAccountCredits = 13278
  println(s"Number of Beneficiaries = BeneAccountDebits + OrigAccountCredits = ${1074+11311}")
  //Number of Beneficiaries = BeneAccountDebits + OrigAccountCredits = 12385

  //OK! I'm satisfied this has been done correctly.



}