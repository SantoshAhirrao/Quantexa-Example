package com.quantexa.example.scoring.scores.fiu.entity

import com.quantexa.scoring.framework.model.scores._
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.example.model.fiu.scoring.EntityAttributes.Business
import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist
import com.quantexa.example.model.fiu.scoring.EdgeAttributes.BusinessEdge
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput

/**
  * This is very similar to [[com.quantexa.example.scoring.scores.fiu.entity.IndividualOnHotlist IndividualOnHotlist]], however
  * it uses EdgeFilter to retrieve those documents which are hotlists.
  * */
object BusinessOnHotlist extends EntityRollUpScore[Business, Hotlist, BasicScoreOutput]
    with EdgeFilter[Business, Hotlist, BasicScoreOutput, BusinessEdge] with HitAggregation {
  
  def createDescription(businessName: String, numberHotlistDocs: Int): String = {
    s"$businessName appears in $numberHotlistDocs hotlist docs."
  }

  val id = "BusinessOnHotlist"
  
  override def score(business: Business, documents: Seq[Hotlist])(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {

    val numberHotlistDocs = documents.size
    //TODO: Work out how EdgeFilter works to be able to remove this check
    if( numberHotlistDocs == 0) {
      None
    }
    else {
      Some(BasicScoreOutput(
        description = createDescription(business.businessNameDisplay.getOrElse("Business"), numberHotlistDocs),
        severity = 100))
    }
  }

  override def edgeFilter(edgeAttributes: BusinessEdge): Boolean = edgeAttributes.businessOnHotlist.contains("true")

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}