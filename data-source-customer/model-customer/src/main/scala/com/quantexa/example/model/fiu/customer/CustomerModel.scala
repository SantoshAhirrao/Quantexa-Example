package com.quantexa.example.model.fiu.customer

import com.quantexa.resolver.ingest.BrokenLenses.BOpticsImplicits.stringIterableString
import com.quantexa.resolver.ingest.Model._
import com.quantexa.resolver.ingest.extractors.DataModelExtractor
import com.quantexa.model.core.datasets.ParsedDatasets._
import com.quantexa.resolver.ingest.BrokenLenses
import com.quantexa.model.core.datasets.ParsedDatasets.{StandardParsedAttributes => attr, StandardParsedCompounds => comp, StandardParsedElements => elem}
import com.quantexa.security.shapeless.Redact
import com.quantexa.security.shapeless.Redact._
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId

import shapeless.Typeable


/***
  * Case class models used to represent the hierarchical model for the FIU Smoke Test Data
  *
  * Stage 2
  * At this stage we should perform data quality checks on our raw data. Profile the data and make decisions as to what
  * our model should be. We should decide what values to nullify and what to do with data that doesn't join our model.
  *
  */
object CustomerModel {
  import com.quantexa.resolver.ingest.WithParent._

  /***
    * Case class to represent an Account in the FIU data
    *
    * @param accountCustomerIdNumber                Primary Key for a Customer
    * @param primaryAccountNumber                  Primary Key for an Account
    * @param accountOpenedDate                Account Opened Date
    * @param accountClosedDate                Account Closed Date
    * @param accountLinkCreatedDate          Account Link Created Date
    * @param accountLinkEndDate              Account Link Destroyed Date
    * @param dataSourceCode              Source Code
    * @param dataSourceCountry             Source Country
    * @param compositeAccountName           Composite Account Name
    * @param consolidatedAccountAddress     Consolidated Account Address
    * @param parsedAddress               Parsed Account Address
    * @param accountStatus                     Status
    * @param accountDistrict                 District
    * @param relationalManagerName                Relational Manager Name
    * @param rawAccountName                 Raw Account Name
    * @param addressLineOne                Address Line 1
    * @param city                         City
    * @param state                        State
    * @param postcode                   Post Code
    * @param country                         Country
    * @param accountRiskRating                Risk Rating
    * @param amlALRFlag                  AML/ALR flag
    * @param telephoneNumber                       Account Telephone Number
    * @param cleansedTelephoneNumber              Cleansed Telephone Number
    */
  case class Account(
    accountCustomerIdNumber: Option[Long],
    @id primaryAccountNumber: Long, //ID is not optional
    accountOpenedDate: Option[java.sql.Date],
    accountClosedDate: Option[java.sql.Date],
    accountLinkCreatedDate: Option[java.sql.Date],
    accountLinkEndDate: Option[java.sql.Date],
    dataSourceCode: Option[String],
    dataSourceCountry: Option[String],
    compositeAccountName: Option[String],
    consolidatedAccountAddress: Option[String],
    parsedAddress: Option[LensParsedAddress[Account]],
    accountStatus: Option[String],
    accountDistrict: Option[String],
    relationalManagerName: Option[String],
    rawAccountName: Option[String],
    addressLineOne: Option[String],
    city: Option[String],
    state: Option[String],
    postcode: Option[String],
    country: Option[String],
    accountRiskRating: Option[Double],
    amlALRFlag: Option[String],
    telephoneNumber: Option[String],
    cleansedTelephoneNumber: Option[LensParsedTelephone[Account]]) extends WithParent[Customer]

  /***
    * Case class to represent a Customer in the FIU data
    * A customer can be either an individual, several individuals or a business
    * @param customerIdNumber                Primary Key for a Customer
    * @param customerStatus                 Status
    * @param registeredBusinessName      Registered Business Name
    * @param parsedBusinessName          Parsed Business Name
    * @param registeredBusinessNumber      Registered Business Number
    * @param countryOfRegisteredBusiness    Country of Registered Business
    * @param customerStartDate             Customer joined Date
    * @param customerEndDate               Customer left Date
    * @param personalBusinessFlag         Personal/Business
    * @param gender                   M/F
    * @param dateOfBirth              Individual Date of Birth
    * @param dateOfBirthParts                Parsed Date of Birth
    * @param initials                 Individual Initials
    * @param forename                 Individual Forename
    * @param familyName              Individual Family Name
    * @param maidenName              Individual Maiden Name
    * @param fullName                Individual Full Name
    * @param parsedCustomerName          Parsed Customer Name
    * @param consolidatedCustomerAddress                Full Customer Address
    * @param parsedAddress           Parsed Customer Address
    * @param nationality              Individual Nationality
    * @param personalNationalIdNumber  Individual National ID Number
    * @param customerRiskRating            Risk Rating
    * @param employeeFlag            Is_Employee?
    * @param residenceCountry           Customer Country of Residence
    * @param accounts                 Seq of Customers Accounts
    */
  case class Customer(
    customerIdNumber: Long, //ID is not optional
    @id customerIdNumberString: String,
    customerStatus: Option[String],
    registeredBusinessName: Option[String],
    parsedBusinessName: Option[LensParsedBusiness[Customer]],
    registeredBusinessNumber: Option[Long],
    countryOfRegisteredBusiness: Option[String],
    customerStartDate: Option[java.sql.Date],
    customerEndDate: Option[java.sql.Date],
    personalBusinessFlag: Option[String],
    gender: Option[String],
    dateOfBirth: Option[java.sql.Date],
    dateOfBirthParts: Option[ParsedDateParts],
    initials: Option[String],
    forename: Option[String],
    familyName: Option[String],
    maidenName: Option[String],
    fullName: Option[String],
    parsedCustomerName: Seq[LensParsedIndividualName[Customer]], //Seq is not optional
    consolidatedCustomerAddress: Option[String],
    parsedAddress: Option[LensParsedAddress[Customer]],
    nationality: Option[String],
    personalNationalIdNumber: Option[Long],
    customerRiskRating: Option[Int],
    employeeFlag: Option[Boolean],
    residenceCountry: Option[String],
    accounts: Seq[Account], //Seq is not optional
    metaData: Option[MetadataRunId])

  object Customer {
    import com.quantexa.security.shapeless.Redact
    import com.quantexa.security.shapeless.Redact.Redactor
    val redactor: Redactor[Customer] = Redact.apply[Customer]
  }
}

object Elements {
  import CustomerModel._
  import BrokenLenses._
  import BOpticOps._

  //Business
  val registeredBusinessName = elementLens[Customer]("registeredBusinessName")(_.registeredBusinessName)
  val parsedBusinessName = elem.businessElements[Customer]
  val registeredBusinessNumber = elementLens[Customer]("registeredBusinessNumber")(_.registeredBusinessNumber.map(_.toString))
  val countryOfRegisteredBusiness = elementLens[Customer]("countryOfRegisteredBusiness")(_.countryOfRegisteredBusiness)
  val customerStartDate = elementLens[Customer]("customerStartDate")(_.customerStartDate.map(_.toString))
  val customerEndDate = elementLens[Customer]("customerEndDate")(_.customerEndDate.map(_.toString))
  val customerStartDateLong = traversal[Customer,java.sql.Date](_.customerStartDate)
  val customerEndDateLong = traversal[Customer,java.sql.Date](_.customerEndDate)

  // Individual
  val customerIdNumber = elementLens[Customer]("customerIdNumber")(_.customerIdNumber.toString)
  val customerStatus = elementLens[Customer]("customerStatus")(_.customerStatus)
  val gender = elementLens[Customer]("gender")(_.gender)
  val fullName = elementLens[Customer]("fullName")(_.fullName)
  val nationality = elementLens[Customer]("nationality")(_.nationality)
  val personalNationalIdNumber = elementLens[Customer]("personalNationalIdNumber")(_.personalNationalIdNumber.map(_.toString))
  val employeeFlag = elementLens[Customer]("employeeFlag")(_.employeeFlag.map(_.toString))
  val residenceCountry = elementLens[Customer]("residenceCountry")(_.residenceCountry)

  // Account
  val primaryAccountNumber = elementLens[Account]("primaryAccountNumber")(_.primaryAccountNumber.toString)
  val accountOpenedDate = elementLens[Account]("accountOpenedDate")(_.accountOpenedDate.map(_.toString))
  val accountClosedDate = elementLens[Account]("accountClosedDate")(_.accountClosedDate.map(_.toString))
  val accountOpenedDateLong = traversal[Account,java.sql.Date](_.accountOpenedDate)
  val accountClosedDateLong = traversal[Account,java.sql.Date](_.accountClosedDate)
  val compositeAccountName = elementLens[Account]("compositeAccountName")(_.compositeAccountName)
  val accountStatus = elementLens[Account]("accountStatus")(_.accountStatus)
  val relationalManagerName = elementLens[Account]("relationalManagerName")(_.relationalManagerName)
  val accountRiskRating = elementLens[Account]("accountRiskRating")(_.accountRiskRating.map(_.toString))
  val amlALRFlag = elementLens[Account]("amlALRFlag")(_.amlALRFlag)

  // Telephone
  val cleansedTelephoneNumber = elem.telephone[Account]

  // Address
  val parsedAddressCustomer = elem.addressElements[Customer]
  val parsedAddressAccount = elem.addressElements[Account]
}

object Compounds {
  import Elements._
  import CustomerModel._
  import com.quantexa.resolver.ingest.BrokenLenses.BOpticOps._
  import com.quantexa.resolver.ingest.BrokenLenses.BOpticsImplicits._

  // Document Attribute Definition
  val customerRiskRatingInt = traversal[Customer, Int](_.customerRiskRating)

  val documentLabels = DocumentAttributeDefinitionList[Customer](
    NamedDocumentAttribute("label",customerIdNumber)
    , NamedDocumentAttribute("customerStartDate",customerStartDateLong)
    , NamedDocumentAttribute("customerEndDate",customerEndDateLong)
    , NamedDocumentAttribute("customerRiskRating",customerRiskRatingInt)
    , NamedDocumentAttribute("nationality",nationality)
    , NamedDocumentAttribute("residenceCountry",residenceCountry)
    , NamedDocumentAttribute("employeeFlag",employeeFlag)
  )

  // Traversals to Entity roots
  //Individual
  val customerToIndividual = traversal[Customer, LensParsedIndividualName[Customer]](_.parsedCustomerName)
  //Account
  val customerToAccount = traversal[Customer, Account](_.accounts)
  //Business
  val customerToBusiness = traversal[Customer,LensParsedBusiness[Customer]](_.parsedBusinessName)
  //Address
  val accountToAddress = traversal[Account, LensParsedAddress[Account]](_.parsedAddress)
  val customerToAddress = traversal[Customer, LensParsedAddress[Customer]](_.parsedAddress)
  //Telephone
  val parsedTelephoneNumber = traversal[Account,LensParsedTelephone[Account]](_.cleansedTelephoneNumber)

  /* ---------------------------------------------------*/
  /*---------------------Individual--------------------*/
  /*--------------------------------------------------*/

  // Compounds
  val individualToCustomer = lens[LensParsedIndividualName[Customer], Customer](_.parent)
  val accountToTelephone = traversal[Account, LensParsedTelephone[Account]](_.cleansedTelephoneNumber)
  val customerToDOB = traversal[Customer, ParsedDateParts](_.dateOfBirthParts)

  val individualToDOB = individualToCustomer ~> customerToDOB
  val individualToBusiness = individualToCustomer ~> customerToBusiness
  val individualToTelephone = individualToCustomer ~> customerToAccount ~> accountToTelephone
  val individualToAccountAddress = individualToCustomer ~> customerToAccount ~> accountToAddress
  val individualToCustomerAddress = individualToCustomer ~> customerToAddress

  //All Compounds/Attributes from Entity Root (LensParsedIndividualName) to (elementLens)
  def individualCustComps = comp.individualCompoundDefs(
    toAddr = Some(individualToCustomerAddress)
    , toDoBParts = Some(individualToDOB)
    , toTel = Some(individualToTelephone)
    , toBiz = Some(individualToBusiness)
  )

  def individualAccComps = comp.individualCompoundDefs(
    toAddr = Some(individualToAccountAddress)
    , toDoBParts = Some(individualToDOB)
    , toTel = Some(individualToTelephone)
    , toBiz = Some(individualToBusiness)
  )

  import StandardParsedElements._

  val individualCompounds = CompoundDefinitionList(
    "individual",
    NamedCompound("forename_surname_primaryAccountNumber", (forename[Customer] ~ surname[Customer]) ~ (individualToCustomer ~> customerToAccount ~> primaryAccountNumber)),
    NamedCompound("personalNationalIdNumber", individualToCustomer ~> personalNationalIdNumber),
    NamedCompound("customerIdNumber", individualToCustomer ~>  customerIdNumber)
  )

  // Attributes
  val `parsedIndividualName..Customer` = lens[LensParsedIndividualName[Customer], Customer](_.parent)
  val customerRiskRatingLong = traversal[Customer, Long](_.customerRiskRating.map(_.toLong))
  val individualDisplay = EntityAttributeDefinitionList(
    "individual"
    , NamedEntityAttribute("fullName", `parsedIndividualName..Customer` ~> fullName)
    , NamedEntityAttribute("personalNationalIdNumber", `parsedIndividualName..Customer` ~> personalNationalIdNumber)
    , NamedEntityAttribute("employeeFlag", `parsedIndividualName..Customer` ~> employeeFlag)
    , NamedEntityAttribute("residenceCountry", `parsedIndividualName..Customer` ~> residenceCountry)
    , NamedEntityAttribute("customerIdNumber", `parsedIndividualName..Customer` ~> customerIdNumber)
    , NamedEntityAttribute("gender", `parsedIndividualName..Customer` ~> gender)
    , NamedEntityAttribute("customerStatus", `parsedIndividualName..Customer` ~> customerStatus)
    , NamedEntityAttribute("nationality", `parsedIndividualName..Customer` ~> nationality)
    , NamedEntityAttribute("customerRiskRating", `parsedIndividualName..Customer` ~> customerRiskRatingLong)
    , NamedEntityAttribute("customerStartDateLong", `parsedIndividualName..Customer` ~> customerStartDateLong)
    , NamedEntityAttribute("customerEndDateLong", `parsedIndividualName..Customer` ~> customerEndDateLong)
  )

  /* ---------------------------------------------------*/
  /*-----------------------Account---------------------*/
  /*--------------------------------------------------*/

  // Compounds
  val accountCompounds = CompoundDefinitionList(
    "account"
    , NamedCompound("primaryAccountNumber", primaryAccountNumber)
  )

  // Attributes
  val accountToCustomer = lens[Account, Customer](_.parent)
  val accountDisplay = EntityAttributeDefinitionList(
    "account"
    , NamedEntityAttribute("label", primaryAccountNumber)
    , NamedEntityAttribute("compositeAccountName", compositeAccountName)
    , NamedEntityAttribute("fullName", accountToCustomer ~> fullName)
    , NamedEntityAttribute("personalNationalIdNumber", accountToCustomer ~> personalNationalIdNumber)
    , NamedEntityAttribute("relationalManagerName", relationalManagerName)
    , NamedEntityAttribute("accountStatus", accountStatus)
    , NamedEntityAttribute("accountRiskRating", accountRiskRating)
    , NamedEntityAttribute("amlALRFlag", amlALRFlag)
    , NamedEntityAttribute("accountOpenedDate", accountOpenedDateLong)
    , NamedEntityAttribute("accountClosedDate", accountClosedDateLong)
  )

  /* ---------------------------------------------------*/
  /*-----------------------Address---------------------*/
  /*--------------------------------------------------*/

  //Compounds
  def addressAccountCompounds = comp.addressCompoundDefs[Account]
  def addressCustomerCompounds = comp.addressCompoundDefs[Customer]

  //Attributes
  def addressAccountAttributes = attr.addressAttributeDefs[Account]
  def addressCustomerAttributes = attr.addressAttributeDefs[Customer]

  /* ---------------------------------------------------*/
  /*-----------------------Business--------------------*/
  /*--------------------------------------------------*/

  // Compounds
  val businessToCustomer = lens[LensParsedBusiness[Customer], Customer](_.parent)

  val businessToAccountAddress = businessToCustomer ~> customerToAccount ~> accountToAddress
  val businessToCustomerAddress = businessToCustomer ~> customerToAddress
  val businessToTelephone = businessToCustomer ~> customerToAccount ~> accountToTelephone

  def businessCustCompounds: CompoundDefinitionList[LensParsedBusiness[Customer]] = comp.businessCompoundDefs(
    toAddress = Some(businessToCustomerAddress)
    , toTelephone = Some(businessToTelephone)
  )

  def businessAccCompounds: CompoundDefinitionList[LensParsedBusiness[Customer]] = comp.businessCompoundDefs(
    toAddress = Some(businessToCustomerAddress)
    , toTelephone = Some(businessToTelephone)
  )

  def businessIDCompounds = CompoundDefinitionList(
    "business",
    NamedCompound("registeredBusinessNumber", businessToCustomer ~> registeredBusinessNumber),
    NamedCompound("primaryAccountNumber", businessNameClean[Customer] ~ (businessToCustomer ~> customerToAccount ~> primaryAccountNumber))
  )

  // Attributes
  val businessDisplay = EntityAttributeDefinitionList(
    "business"
    , NamedEntityAttribute("businessNameDisplay", elem.businessNameDisplay[Customer])
    , NamedEntityAttribute("registeredBusinessName", businessToCustomer ~> registeredBusinessName)
    , NamedEntityAttribute("registeredBusinessNumber", businessToCustomer ~> registeredBusinessNumber)
    , NamedEntityAttribute("countryOfRegisteredBusiness", businessToCustomer ~> countryOfRegisteredBusiness)
    , NamedEntityAttribute("businessJoinedDate", businessToCustomer ~> customerStartDateLong)
    , NamedEntityAttribute("businessLeftDate", businessToCustomer ~> customerEndDateLong)
  )

  val telephoneClean = elementLens[LensParsedTelephone[Account]]("telephoneClean")(_.telephoneClean)
  /* ---------------------------------------------------*/
  /*-----------------------Telephone-------------------*/
  /*--------------------------------------------------*/

  // Compounds
  val telephoneCompounds = CompoundDefinitionList("telephone",
    NamedCompound("telephoneClean", cleansedTelephoneNumber)
  )
  val telephoneDisplays = EntityAttributeDefinitionList("telephone",
    NamedEntityAttribute("telephoneDisplay", elem.telephoneDisplay[Account])
  )

  implicit val CustomerTypeable = Typeable.simpleTypeable(classOf[Customer])
}

import com.quantexa.example.model.fiu.customer.Compounds.CustomerTypeable
object CustomerExtractor extends DataModelExtractor[CustomerModel.Customer](Compounds)
