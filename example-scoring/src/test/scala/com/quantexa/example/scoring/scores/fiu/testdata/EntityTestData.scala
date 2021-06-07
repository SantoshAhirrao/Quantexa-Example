package com.quantexa.example.scoring.scores.fiu.testdata

import com.quantexa.example.model.fiu.scoring.EntityAttributes.{Individual, Telephone, Business}
import com.quantexa.example.scoring.utils.DateParsing

object EntityTestData {
     val testIndividualAtts = Individual(
          customerStartDateLong = Some(DateParsing.parseDate("2012/01/01")),
          customerRiskRating = Some(2000),
          customerEndDateLong = Some(DateParsing.parseDate("2015/01/01")),
          nationality = Some("British"),
          personalNationalIdNumber = Some("12345678"),
          fullName = Some("Dave Burt"),
          gender = Some("Male"),
          employeeFlag = Some("false"),
          residenceCountry = Some("United Kingdom"),
          customerIdNumber = Some("159648"),
          customerStatus = Some("")
     )

     val testPhone = Telephone(
          telephoneDisplay = "075 44887744"
     )
     
     val testBusiness = Business(
          businessLeftDate = Some(DateParsing.parseDate("2012/01/01")),
          registeredBusinessName = Some("Business Name"),
          businessNameDisplay = Some("Business display"),
          registeredBusinessNumber = Some("1234"),
          countryOfRegisteredBusiness = Some("UK"),
          businessJoinedDate = Some(DateParsing.parseDate("2015/01/01"))
     )
}