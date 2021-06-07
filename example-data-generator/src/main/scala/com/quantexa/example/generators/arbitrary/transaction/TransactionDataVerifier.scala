package com.quantexa.example.generators.arbitrary.transaction

import com.quantexa.example.generators.config.GeneratorLoaderOptions
import com.quantexa.example.model.fiu.transaction.TransactionRawModel.TransactionRaw
import org.apache.log4j.Logger
import org.apache.spark.sql.{Dataset, SparkSession}

object TransactionDataVerifier {

  def checkGeneratedTransactions(spark: SparkSession, logger: Logger, generatorConfig: GeneratorLoaderOptions, muleAccount: Long, internalAccounts: Seq[Long], generatedTransactions: Dataset[TransactionRaw]): Unit = {
    import spark.implicits._

    val totalNumberOfTransactions: Long = generatedTransactions.count

    logger.info("There are " + totalNumberOfTransactions + " transactions in the dataset")

    val accountIdsDS = internalAccounts.toDF("accountIds").as[Option[Long]]
    val numberOfTransactionsWithInternalOrigin = generatedTransactions.select("orig_acc_no", "bene_acc_no").join(accountIdsDS, generatedTransactions("orig_acc_no") === accountIdsDS("accountIds"), "inner").select("bene_acc_no").as[Option[Long]]
    val numberOfInternalTransactions = numberOfTransactionsWithInternalOrigin.join(accountIdsDS, numberOfTransactionsWithInternalOrigin("bene_acc_no") === accountIdsDS("accountIds"), "inner").count()

    logger.info("There are " + numberOfInternalTransactions + " internal transactions in the dataset")

    val numberOfMuleAccountTransactions: Long = generatedTransactions
      .filter($"bene_acc_no" === muleAccount)
      .count()

    logger.info("There are " + numberOfMuleAccountTransactions + " mule transactions in the dataset")

    val percentInternalTransactions: Double = numberOfInternalTransactions.toDouble / totalNumberOfTransactions
    val percentExternalTransactions: Double = (totalNumberOfTransactions - numberOfInternalTransactions).toDouble / totalNumberOfTransactions
    val percentMuleTransactions: Double = numberOfMuleAccountTransactions.toDouble / totalNumberOfTransactions

    logger.info("Percentage internal transactions: " + percentInternalTransactions * 100 + "%")
    logger.info("Percentage external transactions: " + percentExternalTransactions * 100 + "%")
    logger.info("Percentage mule transactions: " + percentMuleTransactions * 100 + "%")

    // 20 % of the transactions should be within the bank
    if (0.25 < percentInternalTransactions || 0.15 > percentInternalTransactions) {
      logger.warn("Internal transactions are not 20% of the total transactions: " + percentInternalTransactions * 100 + "%")
    }

    // 80 % of the transactions should be between the bank and an external account
    if (0.85 < percentExternalTransactions || 0.75 > percentExternalTransactions) {
      logger.warn("External transactions are not 80% of the total transactions: " + percentExternalTransactions * 100 + "%")
    }

    // 5 % of transactions should be from a few internal accounts to one external (mule) account
    if (0.07 < percentMuleTransactions || 0.03 > percentMuleTransactions) {
      logger.warn("Mule transactions are not 5% of the total transactions: " + percentMuleTransactions * 100 + "%")
    }
  }
}
