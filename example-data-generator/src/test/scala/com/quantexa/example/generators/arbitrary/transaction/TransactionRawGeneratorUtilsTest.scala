package com.quantexa.example.generators.arbitrary.transaction

import java.sql.Date.valueOf
import scala.util.matching.Regex

import com.quantexa.example.generators.config.{GeneratorConfig, GeneratorLoaderOptions}
import com.quantexa.example.generators.config.GeneratorLoaderImplicits._
import com.quantexa.generators.model.DateRange
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}
import io.circe.config.syntax.CirceConfigOps
import io.circe.generic.auto._
import TransactionRawGeneratorUtils._

class TransactionRawGeneratorUtilsTest  extends FlatSpec with Matchers {

  val testInternalAccounts: Seq[Long] = Seq(1,2,3,4,5,6,7,8,9,10)
  val testInternalAccountsWithTransactions: Seq[Long] = Seq(1,3,4,5,7,8,9)
  val testExternalAccounts: Seq[Long] = Seq(10000,30000,40000,50000,70000,80000,90000)
  val testMuleAccount: Long = 7777
  val testInternalAccountsTargetingMule: Seq[Long] = Seq(1,3,4)
  val testGeneratorLoaderOptions: GeneratorLoaderOptions = ConfigFactory.load("test-config.conf").getConfig("config").as[GeneratorConfig].right.get
  val testGroupedAccountIdsByCustomerWithMoreThanOneAccount: Seq[Seq[Long]] = Seq(Seq(1,2,3), Seq(8,9))

  val testBaseTransactions: Seq[BaseTransaction] =
    accountPairings(testInternalAccounts, testInternalAccountsWithTransactions, testExternalAccounts, testMuleAccount, testInternalAccountsTargetingMule, testGroupedAccountIdsByCustomerWithMoreThanOneAccount, testGeneratorLoaderOptions)

  val transactionDateRange = DateRange(valueOf("2016-08-01"), Some(valueOf("2017-08-01")))

  "AccountPairings" should "create a sequence of base transactions where every currency is either Stirling, Euro or Dollar" in {
    val possibleCurrencies: Regex = "Euro|Stirling|Dollar".r
    val correctCurrencies = testBaseTransactions.filter(baseTransaction => possibleCurrencies.pattern.matcher(baseTransaction.currType).matches())

    testBaseTransactions.size shouldEqual correctCurrencies.size
  }

  "it" should "create a sequence of base transactions where every transaction has at least 1 account from the internal accounts" in {
    val correctAccountPairs = testBaseTransactions.filter(baseTransaction => testInternalAccounts.contains(baseTransaction.accountTo) || testInternalAccounts.contains(baseTransaction.accountFrom))

    testBaseTransactions.size shouldEqual correctAccountPairs.size
  }

  "it" should "create a sequence of base transactions where every date is within August 2016 to August 2017" in {
    val correctDates = testBaseTransactions.filter(baseTransaction => baseTransaction.dt.compareTo(transactionDateRange.from) >= 0 && baseTransaction.dt.compareTo(transactionDateRange.to.get) <= 0)

    testBaseTransactions.size shouldEqual correctDates.size
  }

  "ATMConverter" should "return a value rounded to the nearest 10 if the transaction involves an ATM" in {
    val convertedAmount = atmConverter(155.55, "ATM WITHDRAWAL")

    convertedAmount shouldEqual 160d
  }

  "it" should "return the unchanged amount if the transaction did not involve an ATM" in {
    val convertedAmount = atmConverter(355.55, "POINT OF SALE")

    convertedAmount shouldEqual 355.55
  }

  "it" should "return a maximum of 300 if the transaction involves an ATM" in {
    val convertedAmount = atmConverter(355.55, "ATM WITHDRAWAL")

    convertedAmount shouldEqual 300d
  }
}
