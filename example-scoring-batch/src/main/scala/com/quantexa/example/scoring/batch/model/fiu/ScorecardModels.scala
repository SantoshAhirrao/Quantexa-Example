package com.quantexa.example.scoring.batch.model.fiu

import com.quantexa.scoring.framework.scorecarding.ScoringModels.{ScoreName, WeightedScore}

object ScorecardModels {
  case class WeightedStandardisedScoreOutput(subjectId: Option[String],
                                             scorecardScore: Double,
                                             weightedScoreOutputMap: collection.Map[ScoreName, WeightedScore])
}
