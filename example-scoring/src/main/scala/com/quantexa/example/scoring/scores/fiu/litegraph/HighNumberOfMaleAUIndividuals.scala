package com.quantexa.example.scoring.scores.fiu.litegraph

import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.resolver.core.EntityGraphLite.LiteGraphWithId
import com.quantexa.scoring.framework.model.scores.LiteGraphScore
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{ScoreParameterIdentifier, ScoreParameters, ParameterIdentifier}
import com.quantexa.analytics.scala.graph.LiteGraphUtils._

object HighNumberOfMaleAUIndividuals extends LiteGraphScore[BasicScoreOutput] with ScoreParameters {

  def createDescription(numberMaleAUCustomers: Int, numberMaleAUCustomersThreshold: Int): String = {
    s"There are ${numberMaleAUCustomers} AU males (# hits > ${numberMaleAUCustomersThreshold}) on the network."
  }

  val id = "HighNumberOfMaleAUIndividuals"

  val parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(None,"NumberMaleAUCustomersThreshold"))

  def score(liteGraph: LiteGraphWithId)(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {

    val numberMaleAUCustomersThreshold = parameter[Int]("NumberMaleAUCustomersThreshold")

    val numHits = liteGraph.graph.getEntitiesFromLiteGraphWithAttributes("individual",
      Map("nationality" -> "AU", "gender" -> "M")).distinct.size

    if (numHits >= numberMaleAUCustomersThreshold) {
      Some(BasicScoreOutput(
        description = createDescription(numHits, numberMaleAUCustomersThreshold),
        severity = 100
      ))
    } else None
  }

}