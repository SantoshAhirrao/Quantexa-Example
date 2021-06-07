package com.quantexa.example.scoring.batch.scores.fiu.scorecards

object ScorecardUtils {
  /**
    * Returns the band following a common convention for banding given the severity
    *
    * @param severity Int showing the effect of score
    * @return an optional band between Low/Medium/High
    */
  def defineNormalisedBand(severity: Int): String = {
    severity match {
      case s if s > 75 & s <= 100 => "High"
      case s if s >= 50 & s <= 75 => "Medium"
      case s if s >= 0 & s < 50 => "Low"
    }
  }
}
