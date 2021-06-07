package com.quantexa.example.scoring.scores.fiu.network

import com.quantexa.scoring.framework.model.scores.GraphScore
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.resolver.core.EntityGraph.EntityGraph
import com.quantexa.example.scoring.model.fiu.FiuModelDefinition
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{ScoreParameters, ParameterIdentifier, ScoreParameterIdentifier}

/**
  * TODO: IP-535 Make this a LiteGraphScore
  */
object TotalHighRiskCustomersOnNetwork extends GraphScore[BasicScoreOutput] with ScoreParameters {
  val id = "HighNumberOfHighRiskCustomersOnNetwork"

  def parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(None, "CustomerNetworkLevel_riskRatingThreshold"))

  def scoreGraph(graph: EntityGraph)(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {
    val riskRatingThreshold = parameter[Int]("CustomerNetworkLevel_riskRatingThreshold")

    val docs = scoreInput.documentSources.getOrElse(Map.empty)
    val ratings = docs.map(a => (a._2.get("cus_id_no").toString, a._2.get("cus_risk_rtng").intValue())).toSeq.distinct
    val maxRating = ratings.map(_._2).max
    val counter = ratings.filter(_._2 > riskRatingThreshold).distinct.size

    if (counter > 0)Some(
      BasicScoreOutput(
        description = s"There are ${counter} high risk customers (risk score > ${riskRatingThreshold}) on the network, with a maximum risk score of ${maxRating} ",
        severity = 100
      )
    ) else None
  }
}