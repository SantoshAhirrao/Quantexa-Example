package com.quantexa.example.scoring.scores.fiu.entity

import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, ScoreParameterIdentifier, ScoreParameters}
import com.quantexa.scoring.framework.model.ScoreModel.{BasicScoreOutput, ScoreInput}
import com.quantexa.scoring.framework.model.scores.EntityScore
import com.quantexa.example.scoring.constants.Bands._
import com.quantexa.example.model.fiu.scoring.EntityAttributes.Business
import com.quantexa.example.scoring.utils.TypedConfigReader

/**
  * Score that triggers for recently on-boarded businesses.
  * Banding is as follows:
  * Very High - Joined in the past 7 days
  * High - Joined in past 8 days - 1 month
  * Medium - Joined between 1 month and 2 months ago
  * Low - Joined between 2 and 4 months ago
  * Very Low - Joined between 4 and 6 months ago
  * Does not trigger: Longer than 6 month
  */
object RecentlyJoinedBusiness extends EntityScore[Business, BasicScoreOutput] with ScoreParameters {
  val id = "RecentlyJoinedBusiness"

  val runDate = java.sql.Date.valueOf(TypedConfigReader.getValuesFromConfig().runDate)

  val parameters: Set[ScoreParameterIdentifier] = Set(
    ParameterIdentifier(None, "RecentlyJoinedBusiness_VeryLowSeverity"),
    ParameterIdentifier(None, "RecentlyJoinedBusiness_LowSeverity"),
    ParameterIdentifier(None, "RecentlyJoinedBusiness_MediumSeverity"),
    ParameterIdentifier(None, "RecentlyJoinedBusiness_HighSeverity"),
    ParameterIdentifier(None, "RecentlyJoinedBusiness_VeryHighSeverity")
  )

  def score(business: Business)(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {

    val severitiesForEachBand = Map[String, Int](
      VeryLow -> parameter[Int]("RecentlyJoinedBusiness_VeryLowSeverity"),
      Low -> parameter[Int]("RecentlyJoinedBusiness_LowSeverity"),
      Medium -> parameter[Int]("RecentlyJoinedBusiness_MediumSeverity"),
      High -> parameter[Int]("RecentlyJoinedBusiness_HighSeverity"),
      VeryHigh -> parameter[Int]("RecentlyJoinedBusiness_VeryHighSeverity")
    )

    for {
      joinDate <-business.businessJoinedDate
      recentlyJoinedBand <- getRecencyBandForJoinDate(joinDate)
      severity <- severitiesForEachBand.get(recentlyJoinedBand)
    } yield BasicScoreOutput(
      severity = Some(severity),
      band = Some(recentlyJoinedBand),
      description = Some(createDescription(business.businessNameDisplay.getOrElse("Business"), joinDate))
    )
  }

  private def createDescription(businessName: String, joiningDate: java.sql.Date): String = {
    s"$businessName is a new customer, joining on $joiningDate"
  }

  private def getRecencyBandForJoinDate(businessJoinedDate: java.sql.Date): Option[String] = {
    val joinedDate = businessJoinedDate.toLocalDate
    if (joinedDate.isAfter(runDate.toLocalDate.minusDays(7))) Some(VeryHigh)
    else if (joinedDate.isAfter(runDate.toLocalDate.minusMonths(1))) Some(High)
    else if (joinedDate.isAfter(runDate.toLocalDate.minusMonths(2))) Some(Medium)
    else if (joinedDate.isAfter(runDate.toLocalDate.minusMonths(4))) Some(Low)
    else if (joinedDate.isAfter(runDate.toLocalDate.minusMonths(6))) Some(VeryLow)
    else None
  }

}