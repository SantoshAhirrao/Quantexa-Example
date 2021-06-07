package com.quantexa.example.scoring.scores.fiu.entity

import com.quantexa.example.model.fiu.scoring.EntityAttributes.Individual
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, EntityRollUpScore, HitAggregation}
import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist

/**Rule fires if the individual is connected to a hotlist document.*/
object IndividualOnHotlist extends EntityRollUpScore[Individual,Hotlist,BasicScoreOutput] with HitAggregation {
  
  def createDescription(individualName: String, numberHotlistDocs: Int): String = {
    s"$individualName appears in $numberHotlistDocs hotlist docs"
  }
  
  val id = "IndividualIsOnHotlist"

  def score(ind: Individual, hotlistDocs: Seq[Hotlist])(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {
    
    val numberHotlistDocs = hotlistDocs.size
    
    if (numberHotlistDocs > 0) {
      Some(BasicScoreOutput(
        description = createDescription(ind.fullName.getOrElse("Individual"), numberHotlistDocs),
        severity = 100))
    } else None
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}