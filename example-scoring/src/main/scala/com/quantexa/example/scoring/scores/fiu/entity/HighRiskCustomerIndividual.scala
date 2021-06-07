package com.quantexa.example.scoring.scores.fiu.entity

import scala.util.Try
import com.quantexa.example.model.fiu.scoring.EntityAttributes.Individual
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, EntityScore, HitAggregation}
import com.quantexa.scoring.framework.parameters.{ScoreParameterIdentifier, ScoreParameters, ParameterIdentifier}

/** Triggers if the customer's risk rating exceeds the given threshold. */
object HighRiskCustomerIndividual extends EntityScore[Individual, BasicScoreOutput]
  with ScoreParameters with HitAggregation {

  val id = "HighRiskCustomerIndividual"

  val parameters: Set[ScoreParameterIdentifier] = {
    Set(ParameterIdentifier(None,"HighRiskCustomerIndividual_riskRatingThreshold"))
  }

  def score(ind: Individual)(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {
    val riskRatingExceedsThreshold = ind.customerRiskRating.exists(_ > parameter[Int]("HighRiskCustomerIndividual_riskRatingThreshold"))

    ind.customerRiskRating.flatMap {
      rr =>
        if (riskRatingExceedsThreshold) {
          Some(
            BasicScoreOutput(
              description = s"${ind.fullName} has a high risk rating ($rr)",
              severity = 100
            )
          )
        } else None
    }
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}