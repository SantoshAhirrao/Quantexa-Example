package com.quantexa.example.generators.arbitrary.transaction

import java.sql.Timestamp
import java.sql.Date.valueOf

import scala.util.matching.Regex
import com.quantexa.example.generators.config.GeneratorLoaderOptions
import com.quantexa.generators.model.DateRange
import com.quantexa.generators.utils.GeneratedEntities
import com.quantexa.generators.utils.GeneratorUtils.{defaultFaker, genCategory, genDateRangeBetween, pickRandomFromSequence}
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.functions._
import breeze.stats.distributions._
import scala.collection.Map

object TransactionRawGeneratorUtils {

  case class BaseTransaction(accountFrom: Long, accountTo: Long, amt: Double, currType: String, dt: java.sql.Timestamp)
  case class AccountLookupData(name: String, address: String, country: String)

  def bigTransactionDistribution(seed: Long) = new Gaussian(100, 10000)(RandBasis.withSeed(seed.toInt))
  def smallTransactionDistribution(seed: Long) = new Gaussian(20, 100)(RandBasis.withSeed(seed.toInt))

  val transactionTypesWithoutCounterparty: Regex = "ATM WITHDRAWAL|AUTOMATED CREDIT|ATM DEPOSIT|CHEQUE CREDIT".r
  val transactionTypesWithATM: String = "ATM WITHDRAWAL|ATM DEPOSIT"

  val possibleSuspiciousDates: Seq[Timestamp] = List("01","05").flatMap(month => List("01", "03", "17", "04", "12").map(day => new java.sql.Timestamp(valueOf("2017-" + month + "-" + day).getTime)))
  def selectedSuspiciousDates(seed: Long): Seq[Timestamp] = takeRandom(5, possibleSuspiciousDates, seed)

  val transactionDateRange = DateRange(valueOf("2016-08-01"), Some(valueOf("2017-08-01")))
  def randomTransactionDate(seed: Long) = new Timestamp(genDateRangeBetween(transactionDateRange, seed).sample.get.from.getTime)

  /**
    * Given a sequence and a number, n, chooses n elements from the sequence at random
    * @param n
    * @param sequence
    * @tparam T
    * @return
    */
  def takeRandom[T](n: Int, sequence: Seq[T], seed: Long): Seq[T] = {
    val random = new scala.util.Random(seed)
    for (_ <- 1 to n) yield {
      sequence(random.nextInt(sequence.size))
    }
  }

  /**
    * Generates the base amount for a transaction with 2 decimal places
    * ~95% of the time returns a small value, 5% of the time returns a large transaction value, and 0.1% of time returns an amount in the thousands
    * @return
    */
  private def computeTransactionAmount(seed: Long): Double= {
    val bigTransactionSample = Math.abs(new Gaussian(100, 10000)(RandBasis.withSeed(seed.toInt)).sample)
    val smallTransactionSample = Math.abs(new Gaussian(20, 100)(RandBasis.withSeed(seed.toInt)).sample)
    val transactionAmount = genCategory(Seq((0.001, Math.ceil(bigTransactionSample/1000)*1000), (0.05, bigTransactionSample), (0.949, smallTransactionSample)), seed).sample.get
    Math.round(transactionAmount * 100) / 100.0
  }

  /**
    * Generates a baseTransaction for a transaction targeting the mule account
    * The mule account is characterised by only having transactions sent to it on specific days, in Stirling from specified accounts
    * It can be identified in the csv file by its date timestamp having no time values, only a date (e.g. 17/01/2017T00:00:00.000Z)
    * @param muleAccount
    * @param accountsTargetingMule
    * @return
    */
  private def generateMuleTransaction(muleAccount: Long, accountsTargetingMule: Seq[Long], configSeed: Long, accountSeed: Long): BaseTransaction = {
    BaseTransaction(takeRandom(1, accountsTargetingMule, accountSeed).head, muleAccount, computeTransactionAmount(accountSeed), "Stirling", takeRandom(1, selectedSuspiciousDates(configSeed), accountSeed).head)
  }

  /**
    * Generates a baseTransaction for a transaction where beneficiary and origin accounts belong to the same customer.
    * So, beneficiary and origin are the same customer
    * @param groupedAccountIdsByCustomerWithMoreThanOneAccount
    * @param accountSeed
    * @return
    */
  private def generateTransactionsBetweenTheSameCustomer(groupedAccountIdsByCustomerWithMoreThanOneAccount: Seq[Seq[Long]], accountSeed: Long): BaseTransaction = {
    val randomCustomer: Seq[Long] = takeRandom(1, groupedAccountIdsByCustomerWithMoreThanOneAccount, accountSeed).head
    val randomAccounts: Seq[Long] = takeRandom(2, randomCustomer, accountSeed)

    BaseTransaction(randomAccounts.head, randomAccounts(1), computeTransactionAmount(accountSeed), "Stirling", randomTransactionDate(accountSeed))
  }

  /**
    * For a given internal account number, this will generate a sequence of base transactions based on it
    * @param internalAccNumber
    * @param internalAccounts
    * @param externalAccounts
    * @param muleAccount
    * @param internalAccountsTargetingMule
    * @param config
    * @return
    */
  private def generateBaseTransaction(internalAccNumber: Long,
                              internalAccounts: Seq[Long],
                              externalAccounts: Seq[Long],
                              muleAccount: Long,
                              internalAccountsTargetingMule: Seq[Long],
                              groupedInternalAccountIdsByCustomerWithMoreThanOneAccount: Seq[Seq[Long]],
                              config: GeneratorLoaderOptions):  Seq[BaseTransaction] = {
    val seed = config.seed*internalAccNumber
    val randomGenerator = new scala.util.Random(seed)
    val someExternalAccounts = takeRandom(Math.ceil(randomGenerator.nextDouble()*5).toInt, externalAccounts, seed)
    val specificTransactionTypes = someExternalAccounts.map(extAcc => {
      val extAccSeed = seed*extAcc
      Seq(
        (0.19, BaseTransaction(internalAccNumber, takeRandom(1, internalAccounts, extAccSeed).head, computeTransactionAmount(extAccSeed), "Stirling", randomTransactionDate(extAccSeed))),
        (0.01, generateTransactionsBetweenTheSameCustomer(groupedInternalAccountIdsByCustomerWithMoreThanOneAccount, extAccSeed)),
        (0.375, BaseTransaction(internalAccNumber, extAcc, computeTransactionAmount(extAccSeed), genCategory(config.transactionCurrencies, extAccSeed).sample.get, randomTransactionDate(extAccSeed))),
        (0.375, BaseTransaction(extAcc, internalAccNumber, computeTransactionAmount(extAccSeed), genCategory(config.transactionCurrencies, extAccSeed).sample.get, randomTransactionDate(extAccSeed))),
        (0.05, generateMuleTransaction(muleAccount, internalAccountsTargetingMule, config.seed, extAccSeed))
      )
    })
    //This takes the sequence of sequences of transaction types, and for each set of 4 decides to create between 2 and 6 transactions, picking randomly from the set of 4 each time
    specificTransactionTypes.flatMap(transactionTypes => (1 to Math.ceil(randomGenerator.nextDouble()*5).toInt).map(number => genCategory(transactionTypes, seed*number).sample.get))
  }

  /**
    * For a sequence of account numbers, this will generate a sequence of base transactions based on them
    * @param internalAccounts
    * @param internalAccountsWithTransactions
    * @param externalAccounts
    * @param muleAccount
    * @param internalAccountsTargettingMule
    * @param config
    * @return
    */
  def accountPairings(internalAccounts: Seq[Long],
                      internalAccountsWithTransactions: Seq[Long],
                      externalAccounts: Seq[Long],
                      muleAccount: Long,
                      internalAccountsTargettingMule: Seq[Long],
                      groupedInternalAccountIdsByCustomerWithMoreThanOneAccount: Seq[Seq[Long]],
                      config: GeneratorLoaderOptions): Seq[BaseTransaction] =
    internalAccountsWithTransactions.flatMap(accountNumber => generateBaseTransaction(accountNumber, internalAccounts, externalAccounts, muleAccount, internalAccountsTargettingMule, groupedInternalAccountIdsByCustomerWithMoreThanOneAccount, config))

  /**
    * If the transaction type involves an atm, the amount should be a multiple of ten, with a maximum of 300
    * @param amount
    * @param transactionType
    * @return
    */
  def atmConverter(amount: Double, transactionType: String): Double = {
    if (transactionType.matches(transactionTypesWithATM)) Math.min(Math.ceil(amount / 10) * 10, 300)
    else amount
  }

  /**
    * Given a currency type and a transaction amount in stirling, returns the amount after the exchange rate is applied
    * @param baseAmount
    * @param currency
    * @return
    */
  def currencyConverter(baseAmount: Double, currency: String): Double = currency match {
    case "Dollar" => Math.floor(131 * baseAmount) / 100
    case "Euro"   => Math.floor(116 * baseAmount) / 100
    case _  => baseAmount
  }

  /**
    * For a given account number returns either its looked up name, address and country from the accounts.csv file, and random data if the account cannot be found
    * @param accounts
    * @param accountNumber
    * @param generatedEntities
    * @return
    */
  def lookedUpAccountData(accounts: Map[Long, (String, String, String)], accountNumber: Long, generatedEntities: GeneratedEntities, seed: Long): AccountLookupData = {
    val accountRow = accounts.getOrElse(accountNumber, {
      val randomName = pickRandomFromSequence(generatedEntities.names, seed).fullName
      val randomAddress = pickRandomFromSequence(generatedEntities.addresses, seed)
      val randomFullAddress = randomAddress.fullAddress
      val randomCountryCode = randomAddress.countryCode
      (randomName, randomFullAddress, randomCountryCode)})
    AccountLookupData.tupled(accountRow)
  }

  /**
    * For the beneficiary name, if the transaction type:
    *   -Is without a counterparty: Return no name
    *   -Is a POINT OF SALE: Return a company name
    *   -Is none of the above: Return a standard name
    * @param transactionType
    * @param companyName
    * @param name
    * @return
    */
  def beneNameConverter(transactionType: String, companyName: String, name: Option[String]): Option[String] = {
    transactionType match {
      case transactionTypesWithoutCounterparty() => None
      case "POINT OF SALE" => Some(companyName)
      case _ => name
    }
  }

  /**
    * For the beneficiary address (both address line and country), if the transaction type:
    *   -Is a POINT OF SALE, or has no counterparty: Return no address
    *   -Otherwise: Return the address
    * @param transactionType
    * @param address
    * @return
    */
  def beneAddressConverter(transactionType: String, address: Option[String]): Option[String] = {
    transactionType match {
      case "POINT OF SALE" | transactionTypesWithoutCounterparty() => None
      case _ => address
    }
  }

  /**
    * For the transaction description, if the transaction type:
    *   -Is without a counterparty: Return the transaction type
    *   -Is a POINT OF SALE: Return a company name
    *   -Is none of the above: Return a standard name with the transaction type
    * @param transactionType
    * @param name
    * @return
    */
  def transactionDescriptionCreator(transactionType: String, name: String): String = {
    transactionType match {
      case transactionTypesWithoutCounterparty() => transactionType
      case "POINT OF SALE" => defaultFaker.company().name()
      case _ => name + " (" + transactionType + ")"
    }
  }
}