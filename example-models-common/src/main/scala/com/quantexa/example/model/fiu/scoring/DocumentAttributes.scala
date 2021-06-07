package com.quantexa.example.model.fiu.scoring

import com.quantexa.scoring.framework.model.ScoreDefinition.Model.DocumentAttributes
import java.sql.Date

object DocumentAttributes {

  case class CustomerAttributes(
                                 label: Option[String],
                                 customerRiskRating: Option[Long],
                                 nationality: Option[String],
                                 residenceCountry: Option[String],
                                 employeeFlag: Option[String],
                                 customerStartDate: Option[Date],
                                 customerEndDate: Option[Date]
                               ) extends DocumentAttributes

  case class HotlistAttributes(
                                label: Option[String],
                                dateAdded: Option[Date]
                              ) extends DocumentAttributes

}
