package com.quantexa.example.scoring.batch.scores.fiu.facts

import com.quantexa.analytics.scala.Utils.getFieldNames
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.model.fiu.customer.scoring.ScoreableCustomer
import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.scoring.batch.model.fiu.TransactionInputCustomScore
import com.quantexa.example.scoring.batch.scores.fiu.facts.transactions._
import com.quantexa.example.scoring.model.fiu.FactsModel.{CustomerDateFacts, TransactionFacts}
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scriptrunner.util.DevelopmentConventions.FolderConventions._
import org.apache.spark.sql.catalyst.ScalaReflection.schemaFor
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Dataset, SparkSession}

case class CalculateTransactionFacts(config: ProjectExampleConfig) extends TransactionInputCustomScore {
  def id: String = "TransactionFacts"

  override def dependencies: Set[ScoreModel.Score] = super.dependencies ++ Set( CalculateCustomerDateFacts(config))
  val statisticsToCalculate = Seq(Identity)

  def processScoreableTransaction(spark: SparkSession, transactions: Dataset[ScoreableTransaction])(implicit scoreInput: ScoreInput): Any = {
    import spark.implicits._

    //placeholders for Customer and CustomerDateFacts
    val allStatsApplied: Dataset[TransactionFacts] = statisticsToCalculate.foldLeft(transactions.toDF) {
      case (outDf, stat) => outDf.transform(stat.addFacts)
    }.withColumn("customer", lit(null).cast(schemaFor[Customer].dataType))
      .withColumn("customerDateFacts", lit(null).cast(schemaFor[CustomerDateFacts].dataType))
      .withColumn("paymentToCustomersOwnAccountAtBank", $"counterpartyCustomerId" === $"customerId")
      .as[TransactionFacts]

    //read in Customer and CustomerDateFacts tables
    val scoreableCustomers: Dataset[ScoreableCustomer] = spark.read.parquet(cleansedCaseClassFile(config.hdfsFolderCustomer)).as[Customer].map(customer => ScoreableCustomer(customer))
    val customerDateFacts: Dataset[CustomerDateFacts] = spark.read.parquet(config.hdfsFolderCustomerDateFacts).as[CustomerDateFacts]

    //Join Customer and CustomerDateFacts to TransactionFacts
    val joinedAllFacts1 = allStatsApplied
      .joinWith(scoreableCustomers, scoreableCustomers("customerIdNumber") === allStatsApplied("customerId"), "left")
      .map {
        case (transactionFacts, null) => transactionFacts
        case (transactionFacts, scoreableCustomer) => transactionFacts.copy(customer = scoreableCustomer)
      }

    val joinedAllFacts2 = joinedAllFacts1
      .joinWith(customerDateFacts, customerDateFacts("key.customerId") === joinedAllFacts1("customerId") && customerDateFacts("key.analysisDate") === joinedAllFacts1("analysisDate"), "left")
      .map {
        case (transactionFacts, null) => transactionFacts
        case (transactionFacts, customerDateFacts) => transactionFacts.copy(customerDateFacts = Some(customerDateFacts))
      }

    val transactionFactsColumns = getFieldNames[TransactionFacts].map(col(_))
    val output: Dataset[TransactionFacts] = joinedAllFacts2.select(transactionFactsColumns: _*).as[TransactionFacts]

    output.write.mode("overwrite").parquet(config.hdfsFolderTransactionFacts)
  }
}

