package com.quantexa.example.scoring.scores.fiu.network

import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, GraphScore, HitAggregation}
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.resolver.core.EntityGraph.EntityGraph
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{ScoreParameters, ParameterIdentifier, ScoreParameterIdentifier}

/**
  * TODO: IP-535 Make this a LiteGraphScore
  */
object HighNumberOfHotlistDocs extends GraphScore[BasicScoreOutput] with ScoreParameters with HitAggregation {
  
  def createDescription(numberHotlistDocs: Int, numberHotlistDocsThreshold: Int): String = {
    s"There are ${numberHotlistDocs} hotlist documents (# hotlist docs > ${numberHotlistDocsThreshold}) on the network."
  }
  
  val id = "HighNumberOfHotlistDocs"

  val parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(None, "NumberHotlistDocsThreshold"))

  def scoreGraph(graph: EntityGraph)(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {
    
    val numberHotlistDocsThreshold = parameter[Int]("NumberHotlistDocsThreshold")
    
    val docs = scoreInput.documentSources.getOrElse(Map.empty)
    
    // docs is Map[DocumentId, JsonNode] where DocumentId(`type`: String, value: String) extends NodeId
    // hence we can filter the docs in the network that are Hotlist docs and count them
    val numberHotlistDocs = docs.filter(d => d._1.`type` == "hotlist").toSeq.distinct.size
    
    if (numberHotlistDocs >= numberHotlistDocsThreshold) {
      Some(BasicScoreOutput(
        description = createDescription(numberHotlistDocs, numberHotlistDocsThreshold),    
        severity = 100
      )) 
    } else None
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}