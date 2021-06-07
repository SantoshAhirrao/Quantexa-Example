package com.quantexa.example.model.fiu.scoring

import com.quantexa.scoring.framework.model.ScoreDefinition.Model.EntityAttributes
import java.sql.Date

object EntityAttributes {

  case class Business(
                       businessLeftDate: Option[Date],
                       registeredBusinessName: Option[String],
                       businessNameDisplay: Option[String],
                       registeredBusinessNumber: Option[String],
                       countryOfRegisteredBusiness: Option[String],
                       businessJoinedDate: Option[Date]
                     ) extends EntityAttributes

  case class Individual(
                         customerStartDateLong: Option[Date],
                         customerRiskRating: Option[Long],
                         customerEndDateLong: Option[Date],
                         nationality: Option[String],
                         personalNationalIdNumber: Option[String],
                         fullName: Option[String],
                         gender: Option[String],
                         employeeFlag: Option[String],
                         residenceCountry: Option[String],
                         customerIdNumber: Option[String],
                         customerStatus: Option[String]
                       ) extends EntityAttributes

  case class Address(
                      addressDisplay: String,
                      postcode: Option[String]
                    ) extends EntityAttributes

  case class Telephone(
                        telephoneDisplay: String
                      ) extends EntityAttributes

  case class Account(
                      accountOpenedDate: Option[String],
                      fullName: Option[String],
                      label: Option[String],
                      accountRiskRating: Option[String],
                      accountClosedDate: Option[String],
                      compositeAccountName: Option[String]
                    ) extends EntityAttributes

}
