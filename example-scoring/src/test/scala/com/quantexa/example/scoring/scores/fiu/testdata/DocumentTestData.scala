package com.quantexa.example.scoring.scores.fiu.testdata

import java.text.SimpleDateFormat

import com.quantexa.example.model.fiu.customer.CustomerModel.{Account, Customer}
import com.quantexa.example.model.fiu.customer.scoring.ScoreableCustomer
import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist
import com.quantexa.example.model.fiu.lookupdatamodels.{HighRiskCountryCode, PostcodePrice}
import com.quantexa.example.model.fiu.transaction.TransactionModel.TransactionAccount
import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.model.fiu.scoring.DocumentAttributes.CustomerAttributes
import com.quantexa.model.core.datasets.ParsedDatasets.LensParsedIndividualName

import scala.util.Try

object DocumentTestData {
  val aDate = Try(new SimpleDateFormat("dd/MM/yyyy").parse("01/01/2017"))
    .map(d => new java.sql.Date(d.getTime()))
    .toOption

  val aTimestamp = Try(new SimpleDateFormat("dd/MM/yyyy").parse("01/01/2017"))
    .map(d => new java.sql.Timestamp(d.getTime()))
    .toOption

  val testAccount = Account(
    accountCustomerIdNumber=Some(12345),
    primaryAccountNumber =12345,
    accountOpenedDate = aDate,
    accountClosedDate = aDate,
    accountLinkCreatedDate = aDate,
    accountLinkEndDate = aDate,
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
    customerStartDate = aDate,
    customerEndDate = aDate,
    personalBusinessFlag = Some("flag"),
    gender = Some("gender"),
    dateOfBirth = aDate,
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

  val testHotlist = Hotlist(
    hotlistId = 123456L,
    dateAdded = aDate,
    registeredBusinessName = Some("Business"),
    parsedBusinessName = None,
    personalBusinessFlag = Some("Business flag"),
    dateOfBirth = aDate,
    dateOfBirthParts = None,
    fullName = Some("Full name"),
    parsedIndividualName = Seq.empty,
    consolidatedAddress = Some("Address"),
    parsedAddress = None,
    telephoneNumber = Some("00441111111"),
    cleansedTelephoneNumber = None
  )

  val testScoreableTransaction = ScoreableTransaction(
    transactionId = "123",
    customerId = "456",
    customerName = Some("lisa"),
    debitCredit = "D",
    customerOriginatorOrBeneficiary = Some("originator"),
    postingDate = aDate.orNull,
    analysisDate = aDate.orNull,
    monthsSinceEpoch = 0L,
    amountOrigCurrency = 231.53,
    origCurrency = "GDP",
    amountInGBP = 231.53,
    analysisAmount = 200.00,
    customerAccount = TransactionAccount("456","CustAcc2",None,None,None,None,None,Some("ES"),None),
    counterpartyAccount = Some(TransactionAccount("CP1","CPAcc2",None,None,None,None,None,Some("UK"),None)),
    customerAccountCountry = Some("ES"),
    customerAccountCountryCorruptionIndex = Some(70),
    counterPartyAccountCountry = Some("UK"),
    counterPartyAccountCountryCorruptionIndex = Some(30),
    description = Some("this is a test"),
    counterpartyAccountId = "",
    counterpartyCustomerId = None,
    paymentToCustomersOwnAccountAtBank = None
  )

  val testCustomerAtts = CustomerAttributes(
    label=Some("Dave"),
    customerRiskRating=Some(4501),
    nationality=Some("US"),
    residenceCountry=Some("US"),
    employeeFlag=Some("false"),
    customerStartDate=aDate,
    customerEndDate=aDate)

  val testRiskCountryCodeLookup = HighRiskCountryCode(
    riskCountryCode = "SY"
  )

  val testPostcodePricesLookup = PostcodePrice(
    postcode = "BB97SS",
    price = 100.0
  )
}