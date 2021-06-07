package com.quantexa.example.scoring.batch.generators

import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper._
import org.scalacheck.Gen

import scala.reflect.ClassTag

case object CustomerGenerator extends GeneratesDataset[Customer]  {

   override def generate(implicit ct: ClassTag[Customer]) :Gen[Customer]  = {
    val generator: Gen[Customer] = for {

      customerStatus <- Gen.option(Gen.oneOf("A", "C")) // TODO CHECK IN DATA!!
      registeredBusinessName <- Gen.option(businessNameGen)
      registeredBusinessNumber <- Gen.option(Gen.chooseNum(1l, Int.MaxValue))
      countryOfRegisteredBusiness <- Gen.option(Gen.oneOf(countries)) // TODO CHECK can a business be here!!
      customerStartDateDay <- Gen.chooseNum(0, 31)
      customerStartDateMonth <- Gen.chooseNum(0, 12)
      customerStartDateYear <- Gen.chooseNum(1970, 2020)
      customerStartDate = convertToSqlDate(customerStartDateDay, customerStartDateMonth, customerStartDateYear)
      customerEndDateDay <- Gen.chooseNum(0, 31)
      customerEndDateMonth <- Gen.chooseNum(0, 12)
      customerEndDateYear <- Gen.chooseNum(1970, 2020)
      customerEndDate = convertToSqlDate(customerEndDateDay, customerEndDateMonth, customerEndDateYear)
      personalBusinessFlag <- Gen.option(yesNoGen) // TODO CHECK IN DATA!!
      gender <- Gen.option(genderGen) // TODO CHECK IN DATA!!
      dateOfBirthDay <- Gen.chooseNum(0, 31)
      dateOfBirthMonth <- Gen.chooseNum(0, 12)
      dateOfBirthYear <- Gen.chooseNum(1970, 2020)
      dateOfBirth = convertToSqlDate(dateOfBirthDay, dateOfBirthMonth, dateOfBirthYear)
      initials <- Gen.option(Gen.alphaStr) // TODO CHECK LENGTH!!
      forename <- Gen.option(Gen.oneOf(forenames))
      familyName <- Gen.option(Gen.oneOf(surnames))
      maidenName <- Gen.option(Gen.oneOf(surnames))
      fullName = Option(Seq(forename, maidenName ,familyName).flatten.mkString(" ")).filter(!_.isEmpty)
      consolidatedCustomerAddress <- Gen.option(Gen.alphaNumStr)
      nationality <- Gen.option(countryIsoGen) // TODO CHECK THIS LIST
      personalNationalIdNumber <- Gen.option(Gen.chooseNum(1l, Int.MaxValue)) // TODO CHECK RANGE
      customerRiskRating <- Gen.option(Gen.chooseNum(0, 100)) // TODO CHECK RANGE
      employeeFlag <- maybeBooleanGen //TODO CHECK IN DATA
      residenceCountry <- Gen.option(Gen.oneOf(countries))
      customerIdNumber = (
       customerStatus
      , registeredBusinessName
      , registeredBusinessNumber
      , countryOfRegisteredBusiness
      , customerStartDate
      , customerEndDate
      , personalBusinessFlag
      , gender
      , dateOfBirth
      , initials
      , forename
      , familyName
      , maidenName
      , fullName
      , consolidatedCustomerAddress
      , nationality
      , personalNationalIdNumber
      , customerRiskRating
      , employeeFlag
      , residenceCountry
      ).hashCode()
      accounts <- Gen.listOfN(10, AccountsGenerator(Some(customerIdNumber)).generate )
    } yield
      Customer(customerIdNumber = customerIdNumber
        , customerIdNumberString = customerIdNumber.toString
        , customerStatus = customerStatus
        , registeredBusinessName = registeredBusinessName
        , parsedBusinessName = None
        , registeredBusinessNumber = registeredBusinessNumber
        , countryOfRegisteredBusiness = countryOfRegisteredBusiness
        , customerStartDate = customerStartDate
        , customerEndDate =customerEndDate
        , personalBusinessFlag = personalBusinessFlag
        , gender = gender
        , dateOfBirth = dateOfBirth
        , dateOfBirthParts = None
        , initials = initials
        , forename = forename
        , familyName = familyName
        , maidenName = maidenName
        , fullName = fullName
        , parsedCustomerName = Seq()
        , consolidatedCustomerAddress = consolidatedCustomerAddress
        , parsedAddress = None
        , nationality = nationality
        , personalNationalIdNumber = personalNationalIdNumber
        , customerRiskRating = customerRiskRating
        , employeeFlag = employeeFlag
        , residenceCountry = residenceCountry
        , accounts = accounts
        , metaData = None)

    generator
  }


}
