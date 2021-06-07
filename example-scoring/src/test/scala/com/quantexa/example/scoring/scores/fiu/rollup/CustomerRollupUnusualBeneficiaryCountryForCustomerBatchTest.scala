package com.quantexa.example.scoring.scores.fiu.rollup

import java.sql.{Date => JavaSQLDate}

import com.quantexa.analytics.scala.scoring.model.ScoringModel.ScoreId
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerRollup, TransactionScoreOutput}
import com.quantexa.example.scoring.model.fiu.ScoringModel.{CustomerKey, TransactionKeys}
import com.quantexa.example.scoring.scores.fiu.transaction.UnusualBeneficiaryCountryForCustomer
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{IntegerScoreParameter, ParameterIdentifier}
import monocle.macros.GenLens
import monocle.{Lens, Prism}
import org.scalatest.{FlatSpec, Matchers}

class CustomerRollupUnusualBeneficiaryCountryForCustomerBatchTest extends FlatSpec with Matchers {
  private val maximalSeverityScore = Some(100)
  private val customerIDUnderTest = "Cust002"
  private val scoreId = UnusualBeneficiaryCountryForCustomer.id
  private val analysisDate = JavaSQLDate.valueOf("2018-01-30")
  private val scoreToTest = CustomerRollupUnusualBeneficiaryCountryForCustomerBatch(runDate = analysisDate)

  private val scoreTrigger = TransactionScoreOutput(
    keys = TransactionKeys(
      "Txn002",
      customerIDUnderTest,
      analysisDate),
    severity = Some(99),
    band = None,
    description = None)

  private val noScoresInOutcomePeriod: CustomerRollup[TransactionScoreOutput] = CustomerRollup(
    subject = customerIDUnderTest,
    keys = CustomerKey(customerIDUnderTest),
    customScoreOutputMap = Map.empty
  )

  private val analysisDateLens = GenLens[TransactionScoreOutput](_.keys.analysisDate)
  private val severityLens = GenLens[TransactionScoreOutput](_.severity)
  private val customerRollupCustomerScoreOutputMapLens: Lens[CustomerRollup[TransactionScoreOutput], collection.Map[ScoreId, Seq[TransactionScoreOutput]]] = GenLens[CustomerRollup[TransactionScoreOutput]](_.customScoreOutputMap)

  private val getCustomerRollupFromSeqTransactionScoreOutput = (s: Seq[TransactionScoreOutput]) => customerRollupCustomerScoreOutputMapLens.set(Map(scoreId -> s))(noScoresInOutcomePeriod)

  private val customerRollupCustomScoreOutputMapPrism: Prism[CustomerRollup[TransactionScoreOutput], Seq[TransactionScoreOutput]] = Prism[CustomerRollup[TransactionScoreOutput], Seq[TransactionScoreOutput]] {
    _.customScoreOutputMap.get(scoreId)
  }(getCustomerRollupFromSeqTransactionScoreOutput)

  implicit val scoreInput: ScoreInput = ScoreInput.empty.copy(
    parameters = Map(
      ParameterIdentifier(None, "outcomePeriodMonths") -> IntegerScoreParameter("outcomePeriodMonths", "Outcome Period in months", 1),
      ParameterIdentifier(None, "CustomerRollupUnusualCountryScores_maxNumberOfCountriesInDescription") -> IntegerScoreParameter("CustomerRollupUnusualCountryScores_maxNumberOfCountriesInDescription", "Max number of countries to show in the description", 10)
    )
  )

  "CustomerRollupUnusualBeneficiaryCountryForCustomer" should """take the maximum severity from three scores within the outcome period, and puts the relevant dependent triggers of "Unusual beneficiary for customer" into underlying scores """ in {
    val rollupToTest = customerRollupCustomScoreOutputMapPrism.reverseGet(
      Seq(
        scoreTrigger,
        (analysisDateLens.set(JavaSQLDate.valueOf("2018-01-13")) andThen severityLens.set(Some(50))) (scoreTrigger),
        (analysisDateLens.set(JavaSQLDate.valueOf("2018-01-01")) andThen severityLens.set(maximalSeverityScore)) (scoreTrigger)
      )
    )

    val result = scoreToTest.score(rollupToTest)

    result.get.severity shouldBe maximalSeverityScore
    result.get.underlyingScores should have size 3
  }

  it should  """not trigger if there are triggers for the "Unusual beneficiary for customer" but not in the outcome period """ in {
    val rollupToTest = customerRollupCustomScoreOutputMapPrism.reverseGet(
      Seq(
        (analysisDateLens.set(java.sql.Date.valueOf("2017-01-13")) andThen severityLens.set(Some(50))) (scoreTrigger),
        (analysisDateLens.set(java.sql.Date.valueOf("2017-01-01")) andThen severityLens.set(maximalSeverityScore)) (scoreTrigger)
      ))

    val result = scoreToTest.score(rollupToTest)

    result should not be defined
  }

  it should """not trigger if there are no triggers for  "Unusual beneficiary for customer" """ in {
    val rollupToTest = customerRollupCustomScoreOutputMapPrism.reverseGet(Seq.empty)

    val result = scoreToTest.score(rollupToTest)

    result should not be defined
  }

}
