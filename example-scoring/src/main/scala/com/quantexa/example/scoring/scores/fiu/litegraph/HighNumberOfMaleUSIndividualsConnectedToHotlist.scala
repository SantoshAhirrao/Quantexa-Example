package com.quantexa.example.scoring.scores.fiu.litegraph

import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.resolver.core.EntityGraphLite.LiteGraphWithId
import com.quantexa.scoring.framework.model.scores.LiteGraphScore
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{ScoreParameterIdentifier, ScoreParameters, ParameterIdentifier}
import com.quantexa.analytics.scala.graph.LiteGraphUtils._

object HighNumberOfMaleUSIndividualsConnectedToHotlist extends LiteGraphScore[BasicScoreOutput] with ScoreParameters {

  def createDescription(numberHotlistMaleUSCustomers: Int, numberHotlistMaleUSCustomersThreshold: Int): String = {
    s"There are ${numberHotlistMaleUSCustomers} US males connected to hotlist documents (# hits > ${numberHotlistMaleUSCustomersThreshold}) on the network."
  }

  val id = "HighNumberOfMaleUSIndividualsConnectedToHotlist"

  def parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(None,"NumberHotlistMaleUSCustomersThreshold"))

  def score(liteGraph: LiteGraphWithId)(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {

    val numberHotlistMaleUSCustomersThreshold = parameter[Int]("NumberHotlistMaleUSCustomersThreshold")

    val numHits = liteGraph.graph.getAllEntitiesConnectedToDocType("individual", "hotlist").filter(ent =>
      attributesContainAllSpecifiedValues(ent.attributes, Map("nationality" -> "US", "gender" -> "M"))).distinct.size

    if (numHits >= numberHotlistMaleUSCustomersThreshold) {
      Some(BasicScoreOutput(
        description = createDescription(numHits, numberHotlistMaleUSCustomersThreshold),
        severity = 100
      ))
    } else None
  }

}