package com.quantexa.example.generators.arbitrary.transaction

import java.io.{File, IOException}
import scala.util.Random

import com.quantexa.example.generators.arbitrary.transaction.TransactionRawGeneratorUtils.takeRandom
import com.quantexa.example.generators.config.GeneratorLoaderOptions
import com.quantexa.example.model.fiu.transaction.TransactionRawModel.TransactionRaw
import com.quantexa.generators.templates.Generator
import com.quantexa.generators.utils.GeneratedEntities
import com.quantexa.generators.utils.GeneratorUtils._
import org.scalacheck.Arbitrary
import org.apache.spark.sql.{DataFrame, SparkSession}
import TransactionRawGeneratorUtils._
import com.quantexa.example.model.fiu.customer.CustomerRawModel.AccToCusRaw
import org.apache.spark.sql.functions._

import org.apache.log4j.Logger
import org.apache.spark.sql.types.LongType

import scala.collection.Map

class TransactionRawGenerator(spark: SparkSession, logger: Logger, generatorConfig: GeneratorLoaderOptions, generatedEntities: GeneratedEntities) extends Generator[TransactionRaw] {

  import spark.implicits._

  val accountLookup: DataFrame = {
    if (!new File(generatorConfig.rootHDFSPath + "customer/raw/csv/account.csv").exists) {throw new IOException("Account.csv test data must be generated before the transaction data can be created, please run the customer test data generator script first")}
    spark.read.option("header", "true").csv(generatorConfig.rootHDFSPath + "customer/raw/csv/account.csv").select("prim_acc_no", "composite_acc_name", "consolidated_acc_address", "ctry").withColumn("prim_acc_no", $"prim_acc_no".cast(LongType)).cache
  }

  logger.info("Read in account.csv")

  val internalAccountIds: Seq[Long] = accountLookup.select("prim_acc_no").distinct().as[Long].collect

  logger.info("Read in accountIDs")

  val internalAccountsWithOutboundTransactions: Seq[Long] = takeRandom(Math.round(internalAccountIds.size*0.7).toInt, internalAccountIds, generatorConfig.seed)
  val seededRandom = new scala.util.Random(generatorConfig.seed)
  val poolOfExternalAccounts: Seq[Long] = Seq.fill(Math.round(generatorConfig.numberOfAccountDocuments * 0.75).toInt)(10000000000000l + (seededRandom.nextDouble() * ((99999999999999l - 10000000000000l) + 1)).round)

  val muleAccount: Long = takeRandom(1, poolOfExternalAccounts, generatorConfig.seed).head
  val internalAccountsTargetingMule: Seq[Long] = takeRandom(5, internalAccountIds, generatorConfig.seed)

  val groupedInternalAccountIdsByCustomerWithMoreThanOneAccount: Seq[Seq[Long]] = {
    if (!new File(generatorConfig.rootHDFSPath + "customer/raw/csv/acc_to_cus.csv").exists) {
      throw new IOException("acc_to_cus.csv test data must be generated before the transaction data can be created, please run the customer test data generator script first")
    }

    import spark.implicits._
    spark.read.option("header", "true").option("inferSchema", "true").csv(generatorConfig.rootHDFSPath + "customer/raw/csv/acc_to_cus.csv").as[AccToCusRaw]
      .filter($"cus_id_no".isNotNull && $"prim_acc_no".isNotNull)
      .groupByKey { accToCusRaw =>
        accToCusRaw.cus_id_no.get
      }
      .mapGroups {
        case (_, accounts) =>
          accounts.map(_.prim_acc_no.get).toSeq
      }
      .filter(size($"value") > 1)
      .collect()
      .toSeq
  }

  val generatedBaseTransactions: Seq[BaseTransaction] = accountPairings(internalAccountIds, internalAccountsWithOutboundTransactions, poolOfExternalAccounts, muleAccount, internalAccountsTargetingMule, groupedInternalAccountIdsByCustomerWithMoreThanOneAccount, generatorConfig)

  logger.info("Created " + generatedBaseTransactions.size + " Base Transactions")

  val lookUpTable: Map[Long, (String, String, String)] = accountLookup.rdd.map{ row =>  (row.getLong(0),(row.getString(1),row.getString(2),row.getString(3)))}.collectAsMap()

  logger.info("Created Account Lookup Map")

  def getTransactionRaw: Seq[TransactionRaw] = {
    val arbitraryTransactionRaw = (id: Long) => Arbitrary {
      val seed = new java.util.Random(generatorConfig.seed*id).nextLong
      val baseTransaction = generatedBaseTransactions(id.toInt-1)
      val beneficiaryAccountData = lookedUpAccountData(lookUpTable, baseTransaction.accountTo, generatedEntities, seed)
      val originAccountData = lookedUpAccountData(lookUpTable, baseTransaction.accountFrom, generatedEntities, seed)
      for {
        txn_id <- id.toString.option(1)
        bene_acc_no <- baseTransaction.accountTo.option(1)
        orig_acc_no <- baseTransaction.accountFrom.option(1)
        bene_name <- beneficiaryAccountData.name.option(1)
        bene_ctry <- beneficiaryAccountData.country.option(1)
        bene_addr <- beneficiaryAccountData.address.option(1)
        orig_name <- originAccountData.name.option(1)
        orig_addr <- originAccountData.address.option(1)
        orig_ctry <- originAccountData.country.option(1)
        posting_dt <- baseTransaction.dt.option(1)
        transactionType = genCategory(generatorConfig.transactionTypes, seed).sample.get
        transactionAmount = atmConverter(baseTransaction.amt, transactionType)
        txn_amt <- currencyConverter(transactionAmount, baseTransaction.currType).option(1)
        txn_source_typ_cde <- transactionType.option(1)
        txn_desc <- transactionDescriptionCreator(transactionType, beneficiaryAccountData.name).option(1)
        cr_dr_cde <- (if(transactionType.contains("CREDIT")) "C" else "D").option(1)
        bene_bank_ctry <- beneficiaryAccountData.country.option(1)
        orig_bank_ctry <- originAccountData.country.option(1)
        txn_amt_base <- transactionAmount.option(1)
        run_dt <- baseTransaction.dt.option(1)
        source_sys <- "TXN".option(1)
        source_txn_no <- (genIdLong(seed).sample.get + 1428400000000l).option(1)
        txn_currency <- baseTransaction.currType.option(1)
        base_currency <- "Stirling".option(1)
      } yield TransactionRaw(
        txn_id,
        bene_acc_no,
        orig_acc_no,
        beneNameConverter(transactionType, pickRandomFromSequence(generatedEntities.businesses), bene_name),
        beneAddressConverter(transactionType, bene_ctry),
        beneAddressConverter(transactionType, bene_addr),
        orig_name,
        orig_addr,
        orig_ctry,
        posting_dt,
        txn_amt,
        txn_source_typ_cde,
        txn_desc,
        cr_dr_cde,
        bene_bank_ctry,
        orig_bank_ctry,
        txn_amt_base,
        run_dt,
        source_sys,
        source_txn_no,
        txn_currency,
        base_currency)
      }
    randomWithIds[TransactionRaw](arbitraryTransactionRaw, generatedBaseTransactions.size)
  }

  override def generateRecords : Seq[TransactionRaw] = {
    Random.setSeed(generatorConfig.seed)
    logger.info("Generating Transactions")
    Random.shuffle(getTransactionRaw).take(generatorConfig.numberOfTransactionDocuments)
  }
}
