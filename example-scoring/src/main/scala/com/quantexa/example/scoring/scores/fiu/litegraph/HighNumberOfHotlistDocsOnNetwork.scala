package com.quantexa.example.scoring.scores.fiu.litegraph

import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.resolver.core.EntityGraphLite.LiteGraphWithId
import com.quantexa.scoring.framework.model.scores.LiteGraphScore
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{ScoreParameterIdentifier, ScoreParameters, ParameterIdentifier}
import com.quantexa.analytics.scala.graph.LiteGraphUtils._

object HighNumberOfHotlistDocsOnNetwork extends LiteGraphScore[BasicScoreOutput] with ScoreParameters {

  def createDescription(numberHotlistDocs: Int, numberHotlistDocsThreshold: Int): String = {
    s"There are ${numberHotlistDocs} hotlist documents (# hotlist docs > ${numberHotlistDocsThreshold}) on the network."
  }

  val id = "HighNumberOfHotlistDocsOnNetwork"

  def parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(None,"NumberHotlistDocsThreshold"))

  def score(liteGraph: LiteGraphWithId)(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {

    val numberHotlistDocsThreshold = parameter[Int]("NumberHotlistDocsThreshold")

    val numberHotlistDocs = liteGraph.graph.getDocumentsFromLiteGraphWithAttributes("hotlist").distinct.size

    if (numberHotlistDocs >= numberHotlistDocsThreshold) {
      Some(BasicScoreOutput(
        description = createDescription(numberHotlistDocs, numberHotlistDocsThreshold),
        severity = 100
      ))
    } else None
  }
}