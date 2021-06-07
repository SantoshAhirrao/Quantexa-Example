package com.quantexa.example.scoring.scores.fiu.entity

import java.time.LocalDate

import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import com.quantexa.example.scoring.utils.DateParsing
import com.quantexa.example.scoring.scores.fiu.testdata.EntityTestData.testBusiness
import com.quantexa.example.scoring.constants.Bands._
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, IntegerScoreParameter}

class RecentlyJoinedBusinessTest extends FlatSpec with Matchers with OptionValues{

  val runDate: LocalDate = java.sql.Date.valueOf("2017-08-01").toLocalDate
  val withinWeekDate = Some(DateParsing.parseDate(runDate.withDayOfMonth(runDate.getMonthValue - 1).withDayOfMonth(runDate.getDayOfMonth + 28).toString))
  val withinMonthDate = Some(DateParsing.parseDate(runDate.withMonth(runDate.getMonthValue - 1).withDayOfMonth(runDate.getDayOfMonth + 10).toString))
  val withinTwoMonthsDate = Some(DateParsing.parseDate(runDate.withMonth(runDate.getMonthValue - 2).withDayOfMonth(runDate.getDayOfMonth + 10).toString))
  val withinFourMonthsDate = Some(DateParsing.parseDate(runDate.withMonth(runDate.getMonthValue - 3).toString))
  val withinSixMonthsDate = Some(DateParsing.parseDate(runDate.withMonth(runDate.getMonthValue - 5).toString))
  val withinYearDate = Some(DateParsing.parseDate(runDate.withYear(runDate.getYear - 1).toString))

  implicit val ScoreInputWithParameter = ScoreInput.empty.copy(parameters = Map(
    ParameterIdentifier(None, "RecentlyJoinedBusiness_VeryLowSeverity") -> IntegerScoreParameter("RecentlyJoinedBusiness_VeryLowSeverity", "test", 17),
    ParameterIdentifier(None, "RecentlyJoinedBusiness_LowSeverity") -> IntegerScoreParameter("RecentlyJoinedBusiness_LowSeverity", "test", 34),
    ParameterIdentifier(None, "RecentlyJoinedBusiness_MediumSeverity") -> IntegerScoreParameter("RecentlyJoinedBusiness_MediumSeverity", "test", 50),
    ParameterIdentifier(None, "RecentlyJoinedBusiness_HighSeverity") -> IntegerScoreParameter("RecentlyJoinedBusiness_HighSeverity", "test", 77),
    ParameterIdentifier(None, "RecentlyJoinedBusiness_VeryHighSeverity") -> IntegerScoreParameter("RecentlyJoinedBusiness_VeryHighSeverity", "test", 100)
  ))

  RecentlyJoinedBusiness.id should "trigger have Very High Band and valid severity if business joined between run date and 7 days before" in {
    val testWithinWeekJoinedBusiness =
      RecentlyJoinedBusiness.score(testBusiness.copy(businessJoinedDate = withinWeekDate))

    checkSeverityBetween0And100(testWithinWeekJoinedBusiness)
    getBand(testWithinWeekJoinedBusiness).value shouldBe VeryHigh
  }

  it should "trigger have High Band and valid severity if business joined between 7 days and 1 month before run date" in {
    val testWithinMonthJoinedBusiness =
      RecentlyJoinedBusiness.score(testBusiness.copy(businessJoinedDate = withinMonthDate))

    checkSeverityBetween0And100(testWithinMonthJoinedBusiness)
    getBand(testWithinMonthJoinedBusiness).value shouldBe High
  }
  
  it should "trigger have Medium Band and valid severity if business joined between 1 month and 2 months before run date" in {
    val testWithinTwoMonthsJoinedDate =
      RecentlyJoinedBusiness.score(testBusiness.copy(businessJoinedDate = withinTwoMonthsDate))

    checkSeverityBetween0And100(testWithinTwoMonthsJoinedDate)
    getBand(testWithinTwoMonthsJoinedDate).value shouldBe Medium
  }

  it should "score businesses joining in the last 4 months with L severity" in {
    val testWithinFourMonthsJoinedDate =
      RecentlyJoinedBusiness.score(testBusiness.copy(businessJoinedDate = withinFourMonthsDate))

    checkSeverityBetween0And100(testWithinFourMonthsJoinedDate)
    getBand(testWithinFourMonthsJoinedDate).value shouldBe Low
  }

  it should "score businesses joining in the last 6 months with VL severity" in {
    val testWithinSixMonthsJoinedDate =
      RecentlyJoinedBusiness.score(testBusiness.copy(businessJoinedDate = withinSixMonthsDate))

    checkSeverityBetween0And100(testWithinSixMonthsJoinedDate)
    getBand(testWithinSixMonthsJoinedDate).value shouldBe VeryLow
  }

  it should "not score businesses with no joinedDate" in {
    val testNoJoinedDate =
      RecentlyJoinedBusiness.score(testBusiness.copy(businessJoinedDate = None))

    testNoJoinedDate shouldBe None
  }

  it should "not score businesses joining prior to six months from runDate" in {
    val testOlderThanSixMonths = RecentlyJoinedBusiness.score(
      testBusiness.copy(businessJoinedDate = withinYearDate)
    )

    testOlderThanSixMonths shouldBe None
  }

  private def getSeverity(scoreOutput: Option[BasicScoreOutput]): Option[Int]= {
    scoreOutput.flatMap(_ .severity)
  }

  private def getBand(scoreOutput: Option[BasicScoreOutput]): Option[String] = {
    scoreOutput.flatMap(_.band)
  }

  private def checkSeverityBetween0And100(bso: Option[BasicScoreOutput]){
    val severity = bso.flatMap(_.severity).value
    severity should be <= 100
    severity should be > 0
  }

}