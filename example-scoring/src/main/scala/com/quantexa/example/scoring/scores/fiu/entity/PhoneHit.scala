package com.quantexa.example.scoring.scores.fiu.entity

import com.quantexa.example.model.fiu.scoring.EntityAttributes.Telephone
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.scoring.framework.model.scores._

//TODO: IP-502 Replace this with a better example.
object PhoneHit extends EntityScore[Telephone,BasicScoreOutput] {
  val baseContribution = 10

  override def id: String = "PhoneHit"

  override def score(entity: Telephone)(implicit scoreInput: ScoreModel.ScoreInput): Option[BasicScoreOutput] = {
    Some(
      BasicScoreOutput(
        severity = Some(baseContribution),
        band = None,
        description = Some("They have a Phone!")
      )
    )
  }
}