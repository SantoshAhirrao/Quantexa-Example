package com.quantexa.example.scoring.scores.fiu.rollup

import java.sql.{Date => JavaSQLDate}

import com.quantexa.example.scoring.model.fiu.ScoringModel.{CustomerKey, TransactionKeys}
import com.quantexa.example.scoring.scores.fiu.transaction.UnusualCounterpartyCountryForCustomer
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{IntegerScoreParameter, ParameterIdentifier}
import monocle.Prism
import monocle.macros.GenLens
import org.scalatest.{FlatSpec, Matchers}
import com.quantexa.etl.address.core.Lexicons._
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerRollup, TransactionScoreOutput}

class CustomerRollupUnusualCounterpartyCountryForCustomerBatchTest extends FlatSpec with Matchers  {
  private val maximalSeverityScore = Some(100)
  private val customerIDUnderTest = "Cust002"
  private val scoreId = UnusualCounterpartyCountryForCustomer.id
  private val analysisDate = JavaSQLDate.valueOf("2018-01-30")
  private val scoreToTest  = CustomerRollupUnusualCounterpartyCountryForCustomerBatch(runDate = analysisDate)
  private val countryCode = "NLD"
  private val description = Some("The customer has 3 transactions which were from 1 unusual counterparty countries: NETHERLANDS in the past 1 months")

  private val scoreTrigger = TransactionScoreOutput(
    keys = TransactionKeys(
      "Txn002",
      customerIDUnderTest,
      analysisDate),
    severity = Some(99),
    band = None,
    extraDescriptionText = Map(
      "country" -> CountryISO3ToName.getOrElse(countryCode.toUpperCase, countryCode)),
    description = None)

  private val noScoresInOutcomePeriod: CustomerRollup[TransactionScoreOutput] = CustomerRollup(
    subject = customerIDUnderTest,
    keys = CustomerKey(customerIDUnderTest),
    customScoreOutputMap = Map.empty
  )
  private val analysisDateLens = GenLens[TransactionScoreOutput](_.keys.analysisDate)
  private val severityLens = GenLens[TransactionScoreOutput](_.severity)
  private val customerRollupCustomScoreOutputMapLens = GenLens[CustomerRollup[TransactionScoreOutput]](_.customScoreOutputMap)
  private val customerRollupCustomScoreOutputMapPrism = Prism[CustomerRollup[TransactionScoreOutput], Seq[TransactionScoreOutput]] {
    _.customScoreOutputMap.get(scoreId)
  } { s: Seq[TransactionScoreOutput] => customerRollupCustomScoreOutputMapLens.set(Map(scoreId -> s))(noScoresInOutcomePeriod) }

  implicit val scoreInput: ScoreInput = ScoreInput.empty.copy(
    parameters = Map(
      ParameterIdentifier(None, "outcomePeriodMonths") -> IntegerScoreParameter("outcomePeriodMonths", "Outcome Period in months", 1),
      ParameterIdentifier(None, "CustomerRollupUnusualCountryScores_maxNumberOfCountriesInDescription") -> IntegerScoreParameter("CustomerRollupUnusualCountryScores_maxNumberOfCountriesInDescription", "Max number of countries to show in the description", 10)
    )
  )

  "CustomerRollupUnusualCounterpartyForCustomer" should """take the maximum severity from three scores within the outcome period, and puts the relevant dependent triggers of "Unusual counter party for customer" into underlying scores """ in {
    val rollupToTest = customerRollupCustomScoreOutputMapPrism.reverseGet(
      Seq(
        scoreTrigger,
        (analysisDateLens.set(java.sql.Date.valueOf("2018-01-13")) andThen severityLens.set(Some(50))) (scoreTrigger),
        (analysisDateLens.set(java.sql.Date.valueOf("2018-01-01")) andThen severityLens.set(maximalSeverityScore)) (scoreTrigger)
      ))
    val result = scoreToTest.score(rollupToTest)

    result.get.description shouldBe description
    result.get.severity shouldBe maximalSeverityScore
    result.get.underlyingScores should have size 3
  }

  it should """not trigger if there are triggers for the "Unusual counter party for customer" but not in the outcome period """ in {
    val rollupToTest = customerRollupCustomScoreOutputMapPrism.reverseGet(
      Seq(
        (analysisDateLens.set(java.sql.Date.valueOf("2017-01-13")) andThen severityLens.set(Some(50))) (scoreTrigger),
        (analysisDateLens.set(java.sql.Date.valueOf("2017-01-01")) andThen severityLens.set(maximalSeverityScore)) (scoreTrigger)
      ))
    val result = scoreToTest.score(rollupToTest)
    result should not be defined
  }

  it should """not trigger if there are no triggers for  "Unusual counter party for customer" """ in {
    val rollupToTest = customerRollupCustomScoreOutputMapPrism.reverseGet(Seq.empty)
    val result = scoreToTest.score(rollupToTest)
    result should not be defined
  }
}
