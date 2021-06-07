package com.quantexa.example.scoring.scores.fiu.entity


import com.quantexa.example.model.fiu.scoring.EntityAttributes.Individual
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, EntityRollUpScore, HitAggregation}
import com.quantexa.example.model.fiu.scoring.DocumentAttributes.CustomerAttributes

/**This rule triggers where the individual is resident in multiple countries*/
object IndividualWithMultipleResidenceCountries extends EntityRollUpScore[Individual,CustomerAttributes,BasicScoreOutput]
  with HitAggregation {

  val id = "IndividualWithMultipleResidenceCountries"

  def score(ind: Individual, customerDocs: Seq[CustomerAttributes])(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {
    val customerResidenceCountries = customerDocs.flatMap(_.residenceCountry).distinct
    if (customerResidenceCountries.size > 1) {
      Some(
          BasicScoreOutput(
             description = s"${ind.fullName} has ${customerResidenceCountries.size} different residence countries (${customerResidenceCountries.mkString(",")})",
             severity = 100
          )
      )
    } else None
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}