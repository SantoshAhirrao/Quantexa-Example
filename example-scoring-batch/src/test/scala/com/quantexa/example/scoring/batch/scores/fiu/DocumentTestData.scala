package com.quantexa.example.scoring.batch.scores.fiu


import java.sql.Date
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit

import com.quantexa.example.model.fiu.customer.CustomerModel.{Account, Customer}
import com.quantexa.example.model.fiu.customer.scoring.ScoreableCustomer
import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist
import com.quantexa.example.model.fiu.transaction.TransactionModel.{Transaction, TransactionAccount}
import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.model.fiu.scoring.DocumentAttributes.CustomerAttributes
import com.quantexa.example.scoring.model.fiu.FactsModel.{CategoryCounter, CustomerDateFacts, TransactionFacts, EMAFacts}
import com.quantexa.example.scoring.model.fiu.ScoringModel._
import com.quantexa.model.core.datasets.ParsedDatasets.LensParsedIndividualName
import org.apache.spark.sql.types._

object DocumentTestData {
  private val `dd/MM/yyyy` = new SimpleDateFormat("dd/MM/yyyy")
  private def stringToDate(s:String) = new java.sql.Date(`dd/MM/yyyy`.parse(s).getTime)
  private def stringToTimestamp(s:String) =  new java.sql.Timestamp(`dd/MM/yyyy`.parse(s).getTime)

  val aDate = stringToDate("01/12/2017")
  val bDate = stringToDate("03/12/2017")
  val cDate = stringToDate("06/12/2017")
  val dDate = stringToDate("04/12/2017")
  val fDate = stringToDate("01/12/2017")
  val gDate = stringToDate("03/12/2017")

  val customerAtts = Seq(
      CustomerAttributes(
          label=Some("Dave"),
          customerRiskRating=Some(123L),
          nationality=Some("US"),
          residenceCountry=Some("US"),
          employeeFlag=Some("false"),
          customerStartDate=Some(aDate),
          customerEndDate=Some(bDate)),
      CustomerAttributes(
          label=Some("Aaron"),
          customerRiskRating=Some(123L),
          nationality=Some("US"),
          residenceCountry=Some("UK"),
          employeeFlag=Some("false"),
          customerStartDate=Some(aDate),
          customerEndDate=Some(bDate)),
       CustomerAttributes(
          label=Some("Dave"),
           customerRiskRating=Some(123L),
          nationality=Some("US"),
          residenceCountry=None,
          employeeFlag=Some("false"),
          customerStartDate=Some(bDate),
          customerEndDate=Some(bDate)),
       CustomerAttributes(
          label=Some("Aaron"),
          customerRiskRating=Some(123L),
          nationality=Some("US"),
          residenceCountry=Some("IT"),
          employeeFlag=Some("false"),
          customerStartDate=Some(bDate),
          customerEndDate=Some(bDate))
    )

  val customerDocs: Seq[Customer] = DocumentTestData.customerAtts.zipWithIndex.map {
    case (customer, idx) =>
      Customer(
        customerIdNumber = idx,
        customerIdNumberString = "D" + idx.toString,
        customerStatus = Some("customer status"),
        registeredBusinessName = Some("registered bus name"),
        parsedBusinessName = None,
        registeredBusinessNumber = Some(123),
        countryOfRegisteredBusiness = Some("bus_ctry_registration"),
        customerStartDate = customer.customerStartDate,
        customerEndDate = customer.customerEndDate,
        personalBusinessFlag = Some("flag"),
        gender = Some("gender"),
        dateOfBirth = customer.customerStartDate,
        dateOfBirthParts = None,
        initials = Some("initial"),
        forename = Some("forename"),
        familyName = Some("family_name"),
        maidenName = Some("maiden_name"),
        fullName = Some("full_name"),
        parsedCustomerName = Seq.empty,
        consolidatedCustomerAddress = Some("consolidated_cust_address"),
        parsedAddress = None,
        nationality = customer.nationality,
        personalNationalIdNumber = Some(12345),
        customerRiskRating = customer.customerRiskRating.map(_.toInt),
        employeeFlag = Some(customer.employeeFlag.get.toBoolean),
        residenceCountry = customer.residenceCountry,
        accounts = Seq.empty,
        metaData = None
      )
  }

  val testScoreableTransaction0 = ScoreableTransaction(
    transactionId = "1",
    customerId = "1",
    customerName = Some("Dave"),
    debitCredit = "D",
    customerOriginatorOrBeneficiary = Some("originator"),
    postingDate = aDate,
    analysisDate = aDate,
    monthsSinceEpoch = monthsSinceEpoch(aDate.toLocalDate),
    amountOrigCurrency = 231.53,
    origCurrency = "GDP",
    amountInGBP = 231.53,
    analysisAmount = 200.00,
    customerAccount = TransactionAccount("Cust1","CustAcc1",None,None,None,None,None,Some("ES"),None),
    counterpartyAccount = Some(TransactionAccount("CP1","CPAcc1",None,None,None,None,None,Some("UK"),None)),
    customerAccountCountry = Some("ES"),
    customerAccountCountryCorruptionIndex = Some(70),
    counterPartyAccountCountry = Some("UK"),
    counterPartyAccountCountryCorruptionIndex = Some(30),
    description = Some("scoreable transaction 0"),
    counterpartyAccountId = "",
    counterpartyCustomerId = None,
    paymentToCustomersOwnAccountAtBank = None
  )


  val testScoreableTransaction1 = ScoreableTransaction(
    transactionId = "2",
    customerId = "1",
    customerName = Some("Dave"),
    debitCredit = "D",
    customerOriginatorOrBeneficiary = Some("originator"),
    postingDate = cDate,
    analysisDate = cDate,
    monthsSinceEpoch = monthsSinceEpoch(cDate.toLocalDate),
    amountOrigCurrency = 123.32,
    origCurrency = "GDP",
    amountInGBP = 123.32,
    analysisAmount = 100.00,
    customerAccount = TransactionAccount("Cust1","CustAcc1",None,None,None,None,None,Some("ES"),None),
    counterpartyAccount = Some(TransactionAccount("CP1","CPAcc1",None,None,None,None,None,Some("UK"),None)),
    customerAccountCountry = Some("ES"),
    customerAccountCountryCorruptionIndex = Some(70),
    counterPartyAccountCountry = Some("UK"),
    counterPartyAccountCountryCorruptionIndex = Some(30),
    description = Some("scoreable transaction 1"),
    counterpartyAccountId = "",
    counterpartyCustomerId = None,
    paymentToCustomersOwnAccountAtBank = None
  )

  val testScoreableTransaction2 = ScoreableTransaction(
    transactionId = "3",
    customerId = "1",
    customerName = Some("Dave"),
    debitCredit = "D",
    customerOriginatorOrBeneficiary = Some("originator"),
    postingDate = bDate,
    analysisDate = bDate,
    monthsSinceEpoch = monthsSinceEpoch(bDate.toLocalDate),
    amountOrigCurrency = 231.53,
    origCurrency = "GDP",
    amountInGBP = 231.53,
    analysisAmount = 200.00,
    customerAccount = TransactionAccount("Cust1","CustAcc2",None,None,None,None,None,Some("UK"),None),
    counterpartyAccount = Some(TransactionAccount("CP1","CPAcc2",None,None,None,None,None,Some("ES"),None)),
    customerAccountCountry = Some("UK"),
    customerAccountCountryCorruptionIndex = Some(30),
    counterPartyAccountCountry = Some("ES"),
    counterPartyAccountCountryCorruptionIndex = Some(70),
    description = Some("scoreable transaction 2"),
    counterpartyAccountId = "",
    counterpartyCustomerId = None,
    paymentToCustomersOwnAccountAtBank = None
  )

  val testScoreableTransaction3 = ScoreableTransaction(
    transactionId = "4",
    customerId = "3",
    customerName = Some("Sam"),
    debitCredit = "D",
    customerOriginatorOrBeneficiary = Some("originator"),
    postingDate = fDate,
    analysisDate = fDate,
    monthsSinceEpoch = monthsSinceEpoch(fDate.toLocalDate),
    amountOrigCurrency = 431.53,
    origCurrency = "GDP",
    amountInGBP = 431.53,
    analysisAmount = 400.00,
    customerAccount = TransactionAccount("Cust3","CustAcc1",None,None,None,None,None,Some("ITA"),None),
    counterpartyAccount = Some(TransactionAccount("CP2","CPAcc2",None,None,None,None,None,Some("UK"),None)),
    customerAccountCountry = Some("ITA"),
    customerAccountCountryCorruptionIndex = Some(50),
    counterPartyAccountCountry = Some("UK"),
    counterPartyAccountCountryCorruptionIndex = Some(30),
    description = Some("scoreable transaction 3"),
    counterpartyAccountId = "",
    counterpartyCustomerId = None,
    paymentToCustomersOwnAccountAtBank = None
  )

  val testScoreableTransaction4 = ScoreableTransaction(
    transactionId = "5",
    customerId = "3",
    customerName = Some("Sam"),
    debitCredit = "D",
    customerOriginatorOrBeneficiary = Some("originator"),
    postingDate = gDate,
    analysisDate = gDate,
    monthsSinceEpoch = monthsSinceEpoch(gDate.toLocalDate),
    amountOrigCurrency = 331.53,
    origCurrency = "GDP",
    amountInGBP = 331.53,
    analysisAmount = 300.00,
    customerAccount = TransactionAccount("Cust3","CustAcc1",None,None,None,None,None,Some("ITA"),None),
    counterpartyAccount = Some(TransactionAccount("CP2","CPAcc2",None,None,None,None,None,Some("UK"),None)),
    customerAccountCountry = Some("ITA"),
    customerAccountCountryCorruptionIndex = Some(50),
    counterPartyAccountCountry = Some("UK"),
    counterPartyAccountCountryCorruptionIndex = Some(30),
    description = Some("scoreable transaction 4"),
    counterpartyAccountId = "",
    counterpartyCustomerId = None,
    paymentToCustomersOwnAccountAtBank = None
  )

  val testScoreableTransaction5 = ScoreableTransaction(
    transactionId = "5",
    customerId = "3",
    customerName = Some("Sam"),
    debitCredit = "D",
    customerOriginatorOrBeneficiary = Some("originator"),
    postingDate = gDate,
    analysisDate = gDate,
    monthsSinceEpoch = monthsSinceEpoch(gDate.toLocalDate),
    amountOrigCurrency = 1331.53,
    origCurrency = "GDP",
    amountInGBP = 1331.53,
    analysisAmount = 1300.00,
    customerAccount = TransactionAccount("Cust3","CustAcc1",None,None,None,None,None,Some("ITA"),None),
    counterpartyAccount = Some(TransactionAccount("CP2","CPAcc2",None,None,None,None,None,Some("UK"),None)),
    customerAccountCountry = Some("ITA"),
    customerAccountCountryCorruptionIndex = Some(50),
    counterPartyAccountCountry = Some("UK"),
    counterPartyAccountCountryCorruptionIndex = Some(30),
    description = Some("scoreable transaction 4"),
    counterpartyAccountId = "",
    counterpartyCustomerId = None,
    paymentToCustomersOwnAccountAtBank = None
  )

  val scoreableTransactionsCustomer1 = Seq(testScoreableTransaction0, testScoreableTransaction1, testScoreableTransaction2)
  val scoreableTransactionsCustomer2 = Seq(testScoreableTransaction3, testScoreableTransaction4, testScoreableTransaction5)
  val scoreableTransactionsCustomerMix: Seq[ScoreableTransaction] = Seq(scoreableTransactionsCustomer1, scoreableTransactionsCustomer2).flatten

  val singleCustomerRaw = Seq(("1", "2017-12-01", 1l, 200.0, Map("N/A" -> 1l), Map("UK" -> 1l), 1512086400)
    , ("1", "2017-12-02", 0l, 0.0, Map.empty[String, Long], Map.empty[String, Long], 1512172800)
    , ("1", "2017-12-03", 1l, 200.0, Map("N/A" -> 1l), Map("ES" -> 1l), 1512259200)
    , ("1", "2017-12-04", 0l, 0.0, Map.empty[String, Long], Map.empty[String, Long], 1512345600)
    , ("1", "2017-12-05", 0l, 0.0, Map.empty[String, Long], Map.empty[String, Long], 1512432000)
    , ("1", "2017-12-06", 1l, 100.0, Map("N/A" -> 1l), Map("UK" -> 1l), 1512518400))

  val multipleCustomersRaw = Seq(("3", "2017-12-01", 1l, 400.0, Map("N/A" -> 1l), Map("UK" -> 1l), 1512086400)
    , ("3", "2017-12-02", 0l, 0.0, Map.empty[String, Long], Map.empty[String, Long], 1512172800)
    , ("3", "2017-12-03", 2l, 1600.0, Map("N/A" -> 2l), Map("UK" -> 2l), 1512259200)
    , ("3", "2017-12-04", 0l, 0.0, Map.empty[String, Long], Map.empty[String, Long], 1512345600)
    , ("3", "2017-12-05", 0l, 0.0, Map.empty[String, Long], Map.empty[String, Long], 1512432000)
    , ("3", "2017-12-06", 0l, 0.0, Map.empty[String, Long], Map.empty[String, Long], 1512518400)
    , ("1", "2017-12-01", 1l, 200.0, Map("N/A" -> 1l), Map("UK" -> 1l), 1512086400)
    , ("1", "2017-12-02", 0l, 0.0, Map.empty[String, Long], Map.empty[String, Long], 1512172800)
    , ("1", "2017-12-03", 1l, 200.0, Map("N/A" -> 1l), Map("ES" -> 1l), 1512259200)
    , ("1", "2017-12-04", 0l, 0.0, Map.empty[String, Long], Map.empty[String, Long], 1512345600)
    , ("1", "2017-12-05", 0l, 0.0, Map.empty[String, Long], Map.empty[String, Long], 1512432000)
    , ("1", "2017-12-06", 1l, 100.0, Map("N/A" -> 1l), Map("UK" -> 1l), 1512518400))

  val scoreableTransactionsCustomerEmpty = Seq()

  val rollingBasicStruct = StructType(Seq(StructField("customerId", StringType, nullable = true),
    StructField("analysisDate", DateType, nullable = true),
    StructField("numberOfTransactions", LongType, nullable = false),
    StructField("totalValueTransactions", DoubleType, nullable = false),
    StructField("originatorCountriesPerDay", MapType(StringType, LongType, valueContainsNull = false), nullable = true),
    StructField("beneficiaryCountriesPerDay", MapType(StringType, LongType, valueContainsNull = false), nullable = true),
    StructField("dateTime", IntegerType, nullable = false)))

  val rollingVelueVolumeStruct = StructType(Seq(StructField("customerId", StringType, nullable = true),
    StructField("analysisDate", DateType, nullable = true),
    StructField("numberOfTransactions", LongType, nullable = false),
    StructField("totalValueTransactions", DoubleType, nullable = false),
    StructField("originatorCountriesPerDay", MapType(StringType, LongType, valueContainsNull = false), nullable = true),
    StructField("beneficiaryCountriesPerDay", MapType(StringType, LongType, valueContainsNull = false), nullable = true),
    StructField("dateTime", IntegerType, nullable = false),
    StructField("numberOfTransactionsToDate", LongType, nullable = true),
    StructField("numberOfPreviousTransactionsToDate", LongType, nullable = true),
    StructField("totalTransactionValueToDate", DoubleType, nullable = true),
    StructField("totalPreviousTransactionValueToDate", DoubleType, nullable = true),
    StructField("numberOfTransactionsToday", LongType, nullable = true)))

  val rollingCutomerPrevCountriesStruct = StructType(Seq(StructField("customerId", StringType, nullable = true),
    StructField("analysisDate", DateType, nullable = true),
    StructField("numberOfTransactions", LongType, nullable = false),
    StructField("totalValueTransactions", DoubleType, nullable = false),
    StructField("originatorCountriesPerDay", MapType(StringType, LongType, valueContainsNull = false), nullable = true),
    StructField("beneficiaryCountriesPerDay", MapType(StringType, LongType, valueContainsNull = false), nullable = true),
    StructField("dateTime", IntegerType, nullable = false),
    StructField("numberOfTransactionsToDate", LongType, nullable = true),
    StructField("numberOfPreviousTransactionsToDate", LongType, nullable = true),
    StructField("totalTransactionValueToDate", DoubleType, nullable = true),
    StructField("totalPreviousTransactionValueToDate", DoubleType, nullable = true),
    StructField("numberOfTransactionsToday", LongType, nullable = true),
    StructField("previousOriginatorCountries", MapType(StringType, LongType, valueContainsNull = true), nullable = true),
    StructField("previousBeneficiaryCountries", MapType(StringType, LongType, valueContainsNull = true), nullable = true)))

  def monthsSinceEpoch(date:java.time.LocalDate): Long = {
    val monthsSinceEpoch = java.time.LocalDate.ofEpochDay(0)
    ChronoUnit.MONTHS.between(monthsSinceEpoch, date)
  }

  val testAccount = Account(
    accountCustomerIdNumber=Some(12345),
    primaryAccountNumber =12345,
    accountOpenedDate = Some(aDate),
    accountClosedDate = Some(aDate),
    accountLinkCreatedDate = Some(aDate),
    accountLinkEndDate = Some(aDate),
    dataSourceCode= Some("code"),
    dataSourceCountry = Some("UK"),
    compositeAccountName = Some("accName"),
    consolidatedAccountAddress = Some("123 street"),
    parsedAddress = None,
    accountStatus = Some("A"),
    accountDistrict = Some("accDist"),
    relationalManagerName = Some("mgr"),
    rawAccountName = Some("raw name"),
    addressLineOne = Some("123 Street"),
    city = Some("London"),
    state = Some("London"),
    postcode = Some("AB12 3CD"),
    country = Some("US"),
    accountRiskRating = Some(10.0),
    amlALRFlag = Some("Y"),
    telephoneNumber = Some("00445511 123401"),
    cleansedTelephoneNumber = None
  )

  val testCustomer = Customer(
    customerIdNumber = 123,
    customerIdNumberString = "123",
    customerStatus = Some("A"),
    registeredBusinessName = Some("registered bus name"),
    parsedBusinessName = None,
    registeredBusinessNumber = Some(123),
    countryOfRegisteredBusiness = Some("bus_ctry_registration"),
    customerStartDate = Some(aDate),
    customerEndDate = Some(aDate),
    personalBusinessFlag = Some("flag"),
    gender = Some("gender"),
    dateOfBirth = Some(aDate),
    dateOfBirthParts = None,
    initials = Some("initial"),
    forename = Some("forename"),
    familyName = Some("family_name"),
    maidenName = Some("maiden_name"),
    fullName = Some("full_name"),
    parsedCustomerName = Seq(
      LensParsedIndividualName[Customer](
        nodeId = 157,
        nameDisplay = Some("Dave Burt"),
        forename = Some("Dave"),
        middlename = Some("James"),
        surname = Some("Burt"),
        initial = Some("D")
      )
    ),
    consolidatedCustomerAddress = Some("consolidated_cust_address"),
    parsedAddress = None,
    nationality = Some("UK"),
    personalNationalIdNumber = Some(12345),
    customerRiskRating = Some(123),
    employeeFlag = Some("True".toBoolean),
    residenceCountry = Some("UK"),
    accounts = Seq(testAccount.copy(country=Some("US")),testAccount.copy(country=Some("UK"))),
    metaData = None
  )

  val testScoreableCustomer = ScoreableCustomer(
    customerIdNumber = testCustomer.customerIdNumber,
    customerIdNumberString = testCustomer.customerIdNumberString,
    fullName = testCustomer.fullName,
    customerRiskRating = testCustomer.customerRiskRating
  )

  val testCustomerDateFacts = CustomerDateFacts(
    key = CustomerDateKey(customerId = "123", analysisDate =aDate),
    keyAsString = "yes this is a hack",
    customerName = Some("Jeff Smith"),
    numberOfTransactionsToday =3,
    numberOfTransactionsToDate = 2,
    numberOfPreviousTransactionsToDate = 1,
    numberOfTransactionsOverLast7Days = 2,
    numberOfTransactionsOverLast30Days =2,
    totalValueTransactionsToday= 5,
    totalTransactionValueToDate = 5,
    customerDebitTransactionEMAOverLast30Days = Option(5.0),
    customerDebitTransactionSMAOverLast30Days = None,
    totalPreviousTransactionValueToDate = 10,
    totalValueTransactionsOverLast7Days = 5,
    totalValueTransactionsOverLast30Days = 10,
    transactionValueStdDevToDate = Some(0D),
    previousAverageTransactionVolumeToDate = Some(50002.0),
    previousStdDevTransactionVolumeToDate = Some(50002.0),
    previousAverageTransactionVolumeLast7Days = Some(50003.0),
    previousStdDevTransactionVolumeLast7Days = Some(50003.0),
    previousAverageTransactionVolumeLast30Days = Some(50004.0),
    previousStdDevTransactionVolumeLast30Days = Some(50004.0),
    previousAverageTransactionAmountToDate = Some(49999.0),
    previousStdDevTransactionAmountToDate = Some(49999.0),
    previousAverageTransactionAmountLast7Days = Some(50000.0),
    previousStdDevTransactionAmountLast7Days = Some(50000.0),
    previousAverageTransactionAmountLast30Days = Some(50001.0),
    previousStdDevTransactionAmountLast30Days = Some(50001.0),
    originatorCountriesToday = CategoryCounter(Map("UK" -> 1)),
    beneficiaryCountriesToday = CategoryCounter(Map("US" -> 2))
  )

  val testHotlist = Hotlist(
    hotlistId = 123456L,
    dateAdded = Some(aDate),
    registeredBusinessName = Some("Business"),
    parsedBusinessName = None,
    personalBusinessFlag = Some("Business flag"),
    dateOfBirth = Some(aDate),
    dateOfBirthParts = None,
    fullName = Some("Full name"),
    parsedIndividualName = Seq.empty,
    consolidatedAddress = Some("Address"),
    parsedAddress = None,
    telephoneNumber = Some("00441111111"),
    cleansedTelephoneNumber = None
  )

  val testTransactionFacts = TransactionFacts(
    transactionId = "123",
    customerId = "133",
    customerName = "jeffrey",
    analysisDate = aDate,
    analysisAmount = 231.0,
    amountOrigCurrency = 280.3,
    origCurrency = "EUR",
    customerOriginatorOrBeneficiary = "D",
    customerAccountCountry =Some("ES"),
    customerAccountCountryCorruptionIndex = Some(70),
    counterPartyAccountCountry = Some("UK"),
    counterPartyAccountCountryCorruptionIndex = Some(30),
    customer = testScoreableCustomer,
    customerDateFacts = Some(testCustomerDateFacts),
    paymentToCustomersOwnAccountAtBank = None
  )

  val t1                = Transaction("1",None,None,"debit",java.sql.Timestamp.valueOf("2018-08-28 00:00:00"),
    Date.valueOf("2018-08-28"),1.0,"GBP",0.0,"GBP",None,None,None)
  val t2                = Transaction("1",None,None,"debit",java.sql.Timestamp.valueOf("2018-12-14 00:00:00"),
    Date.valueOf("2018-08-28"),8.0,"GBP",0.0,"GBP",None,None,None)
  val t3                = Transaction("1",None,None,"debit",java.sql.Timestamp.valueOf("2019-01-28 00:00:00"),
    Date.valueOf("2018-08-28"),6.0,"GBP",0.0,"GBP",None,None,None)
  val t4                = Transaction("1",None,None,"debit",java.sql.Timestamp.valueOf("2019-03-28 00:00:00"),
    Date.valueOf("2018-08-28"),14.0,"GBP",0.0,"GBP",None,None,None)

  val testTransactions = Seq(t1,t2,t3,t4)

  val testEMAHelper = EMAFacts(
    key = testCustomerDateFacts.key,
    totalValueTransactionsToday = testCustomerDateFacts.totalValueTransactionsToday,
    customerDebitTransactionSMAOverLast30Days = testCustomerDateFacts.customerDebitTransactionSMAOverLast30Days,
    customerDebitTransactionEMAOverLast30Days = testCustomerDateFacts.customerDebitTransactionEMAOverLast30Days)

  val testSingleSmaFactsInput =  Seq(
    testEMAHelper.copy(totalValueTransactionsToday=10),
    testEMAHelper.copy(totalValueTransactionsToday=5))

  val testSingleSmaFactsOutput = Seq(
    testEMAHelper.copy(totalValueTransactionsToday=10),
    testEMAHelper.copy(totalValueTransactionsToday=5, customerDebitTransactionSMAOverLast30Days = Some(7.5)))

  val testMultipleSmaFactsInput =  Seq(
    testEMAHelper.copy(totalValueTransactionsToday=40),
    testEMAHelper.copy(totalValueTransactionsToday=20),
    testEMAHelper.copy(totalValueTransactionsToday=10),
    testEMAHelper.copy(totalValueTransactionsToday=5))

  val testMultipleSmaFactsOutput = Seq(
    testEMAHelper.copy(totalValueTransactionsToday=40),
    testEMAHelper.copy(totalValueTransactionsToday=20, customerDebitTransactionSMAOverLast30Days = Some(30.0)),
    testEMAHelper.copy(totalValueTransactionsToday=10, customerDebitTransactionSMAOverLast30Days = Some(15.0)),
    testEMAHelper.copy(totalValueTransactionsToday=5, customerDebitTransactionSMAOverLast30Days = Some(7.5)))


  val testSingleEmaFactsInput =  List(
    testEMAHelper.copy(totalValueTransactionsToday=10, customerDebitTransactionEMAOverLast30Days = None, customerDebitTransactionSMAOverLast30Days = Some(5.0)),
    testEMAHelper.copy(totalValueTransactionsToday=5, customerDebitTransactionEMAOverLast30Days = None))

  val testSingleEmaFactsOutput = Seq(
    testEMAHelper.copy(totalValueTransactionsToday=10, customerDebitTransactionEMAOverLast30Days = Some(6.0), customerDebitTransactionSMAOverLast30Days = Some(5.0)),
    testEMAHelper.copy(totalValueTransactionsToday=5, customerDebitTransactionEMAOverLast30Days = Some(5.8)))

  val testMultipleEmaFactsInput =  List(
    testEMAHelper.copy(totalValueTransactionsToday=40, customerDebitTransactionEMAOverLast30Days = None, customerDebitTransactionSMAOverLast30Days = Some(5.0)),
    testEMAHelper.copy(totalValueTransactionsToday=20, customerDebitTransactionEMAOverLast30Days = None),
    testEMAHelper.copy(totalValueTransactionsToday=10, customerDebitTransactionEMAOverLast30Days = None),
    testEMAHelper.copy(totalValueTransactionsToday=5, customerDebitTransactionEMAOverLast30Days = None))

  val testMultipleEmaFactsOutput = Seq(
    testEMAHelper.copy(totalValueTransactionsToday=40, customerDebitTransactionEMAOverLast30Days = Some(12.0), customerDebitTransactionSMAOverLast30Days = Some(5.0)),
    testEMAHelper.copy(totalValueTransactionsToday=20, customerDebitTransactionEMAOverLast30Days = Some(13.6)),
    testEMAHelper.copy(totalValueTransactionsToday=10, customerDebitTransactionEMAOverLast30Days = Some(12.88)),
    testEMAHelper.copy(totalValueTransactionsToday=5, customerDebitTransactionEMAOverLast30Days = Some(11.304)))

  val testTransactionAccounts = TransactionAccount("1", "0123456789", None, None, None, None, None, Some("AU"), None)
}