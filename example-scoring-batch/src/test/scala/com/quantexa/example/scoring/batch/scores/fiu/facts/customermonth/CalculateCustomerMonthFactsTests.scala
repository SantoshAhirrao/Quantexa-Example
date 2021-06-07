package com.quantexa.example.scoring.batch.scores.fiu.facts.customermonth

import java.time.LocalDate

import com.quantexa.example.scoring.batch.tags.SparkTest
import org.scalatest.{FlatSpec, Matchers}
import com.quantexa.example.scoring.batch.scores.fiu.facts.{CalculateCustomerMonthFacts, CustomerDateStatsConfiguration}
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig

@SparkTest
class CalculateCustomerMonthFactsTests extends FlatSpec with Matchers{

  "lastCompleteMonthsSinceEpoch" should "use the latest complete month specified in the runDate in config" in {
    val runDateDt = LocalDate.of(2017,3,1)
    val inCompleteMonthInRunDate = CalculateCustomerMonthFacts(config = ProjectExampleConfig.default("/").copy(runDate = runDateDt.toString),
      stats = CustomerDateStatsConfiguration.empty)

    val custMonthFactsAt15Mar: Long = inCompleteMonthInRunDate.lastCompleteMonthSinceEpoch
    val endOfCalenderMonth: LocalDate = inCompleteMonthInRunDate.monthsSinceEpochToEndOfCalendarMonth(custMonthFactsAt15Mar)
    endOfCalenderMonth shouldBe runDateDt.withDayOfMonth(1)
  }

  it should "use the last complete month when run date is the end of the month" in {
    val runDateDtEndOfMonth = LocalDate.of(2017,3,31)
    val completeMonthsInRunDate = CalculateCustomerMonthFacts(config = ProjectExampleConfig.default("/").copy(runDate = runDateDtEndOfMonth.toString),
      stats = CustomerDateStatsConfiguration.empty)

    val custMonthFactsAt31Mar: Long = completeMonthsInRunDate.lastCompleteMonthSinceEpoch
    val endOfCalenderMonth: LocalDate = completeMonthsInRunDate.monthsSinceEpochToEndOfCalendarMonth(custMonthFactsAt31Mar)
    endOfCalenderMonth shouldBe LocalDate.of(2017,4,1)
  }
}
