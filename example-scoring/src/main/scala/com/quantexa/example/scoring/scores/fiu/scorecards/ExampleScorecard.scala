package com.quantexa.example.scoring.scores.fiu.scorecards

import com.quantexa.example.scoring.scores.fiu.document.customer.{CancelledCustomer, HighRiskCustomerContinuous, HighRiskCustomerDiscrete, NewCustomer}
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.scores.ScoreCardScore
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, ScoreParameterIdentifier}
import com.quantexa.scoring.framework.scorecarding._
import com.quantexa.example.scoring.scores.fiu.rollup._

/**
  * Scorecard to be deployed through UI
  */
object ExampleScorecard extends ScoreCardScore {

  def id: String = "ExampleScoreCard"

  override def scoreCardConfiguration: ScoreCardConfiguration = new DefaultScoreCardConfiguration

  val customerScores: Set[ScoreModel.Score] = Set(HighRiskCustomerDiscrete, NewCustomer, CancelledCustomer)

  //This should be set to current date normally but because of the loaded lookup scores, we need to use this one.
  private def runDate: java.sql.Date = java.sql.Date.valueOf("2017-08-01")

  val rollupScores: Set[ScoreModel.Score] = Set(
    CustomerRollupHighNumberOfRoundAmountOnDemand(runDate),
    CustomerRollupHighToLowRiskCountryOnDemand(runDate),
    CustomerRollupUnusuallyHighDayVolumeOnDemand(runDate),
    CustomerRollupUnusualBeneficiaryCountryForCustomerOnDemand(runDate),
    CustomerRollupUnusualCounterpartyCountryForCustomerOnDemand(runDate),
    CustomerRollupUnusualOriginatingCountryForCustomerOnDemand(runDate)
  )

  val scoresToScorecard: Set[ScoreModel.Score] = customerScores ++ rollupScores

  override def dependencies: Set[ScoreModel.Score] = scoresToScorecard

  def groupParameters = dependencies.map(score => ParameterIdentifier(None, score.id))

  override def parameters: Set[ScoreParameterIdentifier] = groupParameters.map(_.asInstanceOf[ScoreParameterIdentifier])

}
