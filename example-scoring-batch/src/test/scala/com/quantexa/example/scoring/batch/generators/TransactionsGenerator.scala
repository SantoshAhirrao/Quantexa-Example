package com.quantexa.example.scoring.batch.generators

import com.quantexa.example.model.fiu.customer.CustomerModel
import com.quantexa.example.model.fiu.customer.CustomerModel.Account
import com.quantexa.example.model.fiu.transaction.TransactionModel.{Transaction, TransactionAccount}
import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper._
import org.scalacheck.Gen

import scala.reflect.ClassTag

case class TransactionsGenerator(transactionAccountsGenerator: TransactionAccountGenerator) extends GeneratesDataset[Transaction] {

  override def generate(implicit ct: ClassTag[Transaction]): Gen[Transaction] = for {
    originatingAccount <- transactionAccountsGenerator.generate
    beneficiaryAccount <- transactionAccountsGenerator.generate
    debitCredit <- Gen.oneOf("D", "C")
    postingDayDateDay <- Gen.chooseNum(1, 31)
    postingDayDateMonth <- Gen.chooseNum(1, 12)
    postingDayDateYear <- Gen.chooseNum(2016, 2017)
    postingDayDateHourOfDay <- Gen.chooseNum(0, 23)
    postingDayDateMinute <- Gen.chooseNum(0, 59)
    postingDayDateSecond <- Gen.chooseNum(0, 59)
    txnAmount <- Gen.chooseNum(0d, 1000000d)
    txnCurrency <- Gen.oneOf(currencies)
    txnAmountInBaseCurrency <- Gen.chooseNum(0d, 1000000d)
    txnDescription <- Gen.option(Gen.oneOf("desc1", "desc2"))
    baseCurrency = "GBP"
    sourceSystem <- Gen.option(Gen.oneOf("src1", "src2"))
    transactionId = (
      originatingAccount,
      beneficiaryAccount,
      debitCredit,
      postingDayDateDay,
      postingDayDateMonth,
      postingDayDateYear,
      txnAmount,
      txnCurrency,
      txnAmountInBaseCurrency,
      txnDescription,
      baseCurrency,
      sourceSystem).hashCode()
  } yield
    Transaction(transactionId = transactionId.toString
      , originatingAccount = Some(originatingAccount)
      , beneficiaryAccount = Some(beneficiaryAccount)
      , debitCredit = debitCredit
      , postingDate = convertToSqlTimestamp(postingDayDateDay, postingDayDateMonth, postingDayDateYear, postingDayDateHourOfDay, postingDayDateMinute, postingDayDateSecond).get
      , runDate = convertToSqlDate(postingDayDateDay, postingDayDateMonth, postingDayDateYear).get //Set runDate and postingDate the same.
      , txnAmount = txnAmount
      , txnCurrency = txnCurrency
      , txnAmountInBaseCurrency = txnAmountInBaseCurrency
      , baseCurrency = baseCurrency
      , txnDescription = txnDescription
      , sourceSystem = sourceSystem
      , metaData = None)

  def guaranteedTransactions(account1:Account, account2:Account): List[Transaction] = {
    val acc1 = TransactionAccount(account1.primaryAccountNumber.toString, account1.primaryAccountNumber.toString, None, None, None, None, None, None, None)
    val acc2 = TransactionAccount(account2.primaryAccountNumber.toString, account2.primaryAccountNumber.toString, None, None, None, None, None, None, None)
    val exampleTxn = Transaction(transactionId = "txn001"
      , originatingAccount = Some(acc1)
      , beneficiaryAccount = Some(acc2)
      , debitCredit = "D"
      , postingDate = convertToSqlTimestamp(1, 7, 2017, 12, 11, 10).get
      , runDate = convertToSqlDate(1, 7, 2017).get //Set runDate and postingDate the same.
      , txnAmount = 10000D
      , txnCurrency = "GBP"
      , txnAmountInBaseCurrency = 10000D
      , baseCurrency = "GBP"
      , txnDescription = None
      , sourceSystem = None
      , metaData = None)
    List(exampleTxn,exampleTxn.copy(transactionId="txn002", txnAmount = 35000D, txnAmountInBaseCurrency = 35000D))
  }

  def transactionsBetweenTheSameCustomer(customers: Seq[CustomerModel.Customer]): Seq[Transaction] = {
    customers.map { customer =>
      val account1 = customer.accounts.minBy(_.primaryAccountNumber)
      val account2 = getAccountForTheSameCustomer(account1, customer.accounts)

      val acc1 = TransactionAccount(account1.primaryAccountNumber.toString, account1.primaryAccountNumber.toString, None, None, None, None, None, None, None)
      val acc2 = TransactionAccount(account2.primaryAccountNumber.toString, account2.primaryAccountNumber.toString, None, None, None, None, None, None, None)

      Transaction(transactionId = customer.hashCode().toString
        , originatingAccount = Some(acc1)
        , beneficiaryAccount = Some(acc2)
        , debitCredit = "D"
        , postingDate = convertToSqlTimestamp(1, 7, 2017, 12, 11, 10).get
        , runDate = convertToSqlDate(1, 7, 2017).get //Set runDate and postingDate the same.
        , txnAmount = 10000D
        , txnCurrency = "GBP"
        , txnAmountInBaseCurrency = 10000D
        , baseCurrency = "GBP"
        , txnDescription = None
        , sourceSystem = None
        , metaData = None)
    }
  }

  private def getAccountForTheSameCustomer(account: Account, accounts: Seq[Account]): Account = {
    accounts.filter(acc => acc.primaryAccountNumber != account.primaryAccountNumber && acc.accountCustomerIdNumber == account.accountCustomerIdNumber)
      .minBy(_.primaryAccountNumber)
  }
}