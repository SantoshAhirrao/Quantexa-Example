package com.quantexa.example.generators.config

import scala.util.Random

case class GeneratorLoaderOptions(

                                   numberOfCustomerDocuments : Int,
                                   numberOfAccountDocuments: Int,
                                   numberOfHotlistDocuments: Int,
                                   numberOfTransactionDocuments: Int,
                                   numberOfPersonEntities: Int,
                                   numberOfBusinessEntities: Int,
                                   numberOfAddressEntities: Int,
                                   numberOfPhoneEntities: Int,
                                   numberOfEmailEntities: Int,
                                   rootHDFSPath : String,
                                   familyNameChanged: Double,
                                   isEmployee: Double,
                                   amlAlrFlagged: Double,
                                   accountStates: Seq[(Double,String)],
                                   dataSourceCodes: Seq[(Double,String)],
                                   accountTypes: Seq[(Double,String)],
                                   customerState: Seq[(Double,String)],
                                   possibleGenders: Seq[(Double,String)],
                                   countryCodes: Seq[(Double,String)],
                                   transactionCurrencies: Seq[(Double,String)],
                                   transactionTypes: Seq[(Double,String)],
                                   seed: Int
  )

object GeneratorLoaderImplicits {

  implicit def generatorConfigToLoaderOptions(tsConfig: GeneratorConfig): GeneratorLoaderOptions = {
    val numberOfCustomers = tsConfig.numberOfCustomerDocuments.getOrElse(100)
    val numberOfAccounts = tsConfig.numberOfAccountDocuments.getOrElse(100)
    val numberOfHotlists = tsConfig.numberOfHotlistDocuments.getOrElse(20)
    val numberOfTransactions = tsConfig.numberOfTransactionDocuments.getOrElse(50)
    val numberOfPeople = tsConfig.numberOfPersonEntities.getOrElse(60)
    val numberOfBusinesses = tsConfig.numberOfBusinessEntities.getOrElse(60)
    val numberOfAddresses = tsConfig.numberOfAddressEntities.getOrElse(60)
    val numberOfPhones = tsConfig.numberOfPhoneEntities.getOrElse(60)
    val numberOfEmails = tsConfig.numberOfEmailEntities.getOrElse(60)
    val rootHDFSPath = tsConfig.rootHDFSPath
    val familyNameChanged = tsConfig.familyNameChanged.getOrElse(1d)
    val isEmployee = tsConfig.isEmployee.getOrElse(1d)
    val amlAlrFlagged = tsConfig.amlAlrFlagged.getOrElse(1d)
    val accountStates = getProbabilityTuples(tsConfig.accountStates)
    val dataSourceCodes = getProbabilityTuples(tsConfig.dataSourceCodes)
    val accountTypes = getProbabilityTuples(tsConfig.accountTypes)
    val customerState = getProbabilityTuples(tsConfig.customerState)
    val possibleGenders = getProbabilityTuples(tsConfig.possibleGenders)
    val countryCodes = getProbabilityTuples(tsConfig.countryCodes)
    val transactionCurrencies = getProbabilityTuples(tsConfig.transactionCurrencies)
    val transactionTypes = getProbabilityTuples(tsConfig.transactionTypes)
    val seed = tsConfig.seed.getOrElse(new Random().nextInt())

    GeneratorLoaderOptions(
      numberOfCustomers,
      numberOfAccounts,
      numberOfHotlists,
      numberOfTransactions,
      numberOfPeople,
      numberOfBusinesses,
      numberOfAddresses,
      numberOfPhones,
      numberOfEmails,
      rootHDFSPath,
      familyNameChanged,
      isEmployee,
      amlAlrFlagged,
      accountStates,
      dataSourceCodes,
      accountTypes,
      customerState,
      possibleGenders,
      countryCodes,
      transactionCurrencies,
      transactionTypes,
      seed
    )
  }

  private def getProbabilityTuples(string : String) : Seq[(Double,String)] = {
    string.split("[\\(||\\)]").filter(!_.isEmpty).map(str => {
      val values = str.split(",")
      (values.head.toDouble,values.last)
    }).toSeq
  }
}