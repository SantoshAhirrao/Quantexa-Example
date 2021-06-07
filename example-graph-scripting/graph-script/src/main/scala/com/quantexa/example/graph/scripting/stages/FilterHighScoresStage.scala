package com.quantexa.example.graph.scripting.stages

import com.quantexa.graph.script.clientrequests.{FutureErrorOr, FutureErrorOrWithMetrics}
import com.quantexa.graph.script.metrics.LocalStepMetrics
import com.quantexa.graph.script.utils.EntityGraphWithScore
import com.quantexa.graph.script.utils.GraphScoringUtils.sumScoreOutputs

import scala.concurrent.{ExecutionContext, Future}

object FilterHighScoresStage {

  val stageName = "FilterHighScoresStage"

  /**
    * This stage filters entityGraphWithScore to graphs above a threshold
    */
  def apply(entityGraphWithScore: EntityGraphWithScore, threshold: Float)(implicit ec: ExecutionContext): FutureErrorOrWithMetrics[EntityGraphWithScore] = {
    import cats.implicits._

    for {
      filtered <- filterScoreOverThresholdStep(entityGraphWithScore, threshold)
    } yield filtered

  }

  def filterScoreOverThresholdStep(entityGraphWithScore: EntityGraphWithScore, threshold: Float): FutureErrorOrWithMetrics[EntityGraphWithScore] = {
    //TODO: Possibly can remove FurtureErrorOr* wrapping
    new FutureErrorOrWithMetrics(
      new FutureErrorOr(
        if(sumScoreOutputs(entityGraphWithScore.scoreOutputs) > threshold){
          Future.successful(Right((List.empty[LocalStepMetrics], entityGraphWithScore)))
        } else {
          Future.successful(Left(s"Did not score over $threshold"))
        })
    )
  }
}
