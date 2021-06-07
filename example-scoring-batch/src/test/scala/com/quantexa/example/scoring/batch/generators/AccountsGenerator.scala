package com.quantexa.example.scoring.batch.generators

import com.quantexa.example.model.fiu.customer.CustomerModel.Account
import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper._
import org.scalacheck.Gen

import scala.reflect.ClassTag
case class AccountsGenerator(customerId : Option[Long]) extends GeneratesDataset[Account]{
 // accountCustomerIdNumber + primaryAccountNumber should be unique - primaryAccountNumber is now unique because it is a hash of all variable fields
  override def generate(implicit ct: ClassTag[Account]): Gen[Account] = {
    val generator: Gen[Account] = for {
      accountOpenedDateDay <- Gen.chooseNum(0, 31)
      accountOpenedDateMonth <- Gen.chooseNum(0, 12)
      accountOpenedDateYear <- Gen.chooseNum(1970, 2020)
      accountOpenedDate = convertToSqlDate(accountOpenedDateDay, accountOpenedDateMonth, accountOpenedDateYear)
      accountClosedDateDay <- Gen.chooseNum(0, 31)
      accountClosedDateMonth <- Gen.chooseNum(0, 12)
      accountClosedDateYear <- Gen.chooseNum(1970, 2020)
      accountClosedDate = convertToSqlDate(accountClosedDateDay, accountClosedDateMonth, accountClosedDateYear)
      accountLinkCreatedDateDay <- Gen.chooseNum(0, 31)
      accountLinkCreatedDateMonth <- Gen.chooseNum(0, 12)
      accountLinkCreatedDateYear <- Gen.chooseNum(1970, 2020)
      accountLinkCreatedDate = convertToSqlDate(accountLinkCreatedDateDay, accountLinkCreatedDateMonth, accountLinkCreatedDateYear)
      accountLinkEndDateDay <- Gen.chooseNum(0, 31)
      accountLinkEndDateMonth <- Gen.chooseNum(0, 12)
      accountLinkEndDateYear <- Gen.chooseNum(1970, 2020)
      accountLinkEndDate = convertToSqlDate(accountLinkEndDateDay, accountLinkEndDateMonth, accountLinkEndDateYear)
      dataSourceCode <- Gen.option(Gen.alphaStr)
      dataSourceCountry <- Gen.option(Gen.oneOf(countries))
      compositeAccountName <- Gen.option(fullnameGen)
      consolidatedAccountAddress <- Gen.option(Gen.alphaNumStr)
      accountStatus <- Gen.option(Gen.oneOf("active", "deactivated"))
      accountDistrict <- Gen.option(Gen.alphaNumStr)
      relationalManagerName <- Gen.option(Gen.alphaStr)
      rawAccountName <- Gen.option(Gen.alphaNumStr)
      addressLineOne <- Gen.option(Gen.alphaNumStr)
      city <- Gen.option(Gen.alphaStr)
      state <- Gen.option(Gen.alphaStr)
      postcode <- Gen.option(Gen.alphaNumStr)
      country <- Gen.option(Gen.oneOf(countries))
      accountRiskRating <- Gen.option(Gen.chooseNum(0, 10000d) )// TODO CHECK IN DATA
      amlALRFlag <- Gen.option(yesNoGen)
      telephoneNumber <- Gen.option(Gen.numStr)
      primaryAccountNumber = (
        customerId
        , accountOpenedDate
        , accountClosedDate
        , accountLinkCreatedDate
        , accountLinkEndDate
        , dataSourceCode
        , dataSourceCountry
        , compositeAccountName
        , consolidatedAccountAddress
        , accountStatus
        , accountDistrict
        , relationalManagerName
        , rawAccountName
        , addressLineOne
        , city
        , state
        , postcode
        , country
        , accountRiskRating
        , amlALRFlag
        , telephoneNumber
        ).hashCode()
    } yield Account(
      accountCustomerIdNumber = customerId
      , primaryAccountNumber = primaryAccountNumber
      , accountOpenedDate = accountOpenedDate
      , accountClosedDate = accountClosedDate
      , accountLinkCreatedDate = accountLinkCreatedDate
      , accountLinkEndDate = accountLinkEndDate
      , dataSourceCode = dataSourceCode
      , dataSourceCountry = dataSourceCountry
      , compositeAccountName = compositeAccountName
      , consolidatedAccountAddress = consolidatedAccountAddress
      , parsedAddress = None
      , accountStatus = accountStatus
      , accountDistrict = accountDistrict
      , relationalManagerName = relationalManagerName
      , rawAccountName = rawAccountName
      , addressLineOne = addressLineOne
      , city = city
      , state = state
      , postcode = postcode
      , country = country
      , accountRiskRating = accountRiskRating
      , amlALRFlag = amlALRFlag
      , telephoneNumber = telephoneNumber
      , cleansedTelephoneNumber = None)
    generator
  }
}
