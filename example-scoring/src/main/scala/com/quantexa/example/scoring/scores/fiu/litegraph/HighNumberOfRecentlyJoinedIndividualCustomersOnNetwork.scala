package com.quantexa.example.scoring.scores.fiu.litegraph

import java.sql.Date
import com.quantexa.example.scoring.utils.DateParsing
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.resolver.core.EntityGraphLite.{LiteEntity, LiteGraphWithId}
import com.quantexa.scoring.framework.model.scores.LiteGraphScore
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{ScoreParameterIdentifier, ScoreParameters, ParameterIdentifier}
import com.quantexa.analytics.scala.graph.LiteGraphUtils._

object HighNumberOfRecentlyJoinedIndividualCustomersOnNetwork extends LiteGraphScore[BasicScoreOutput] with ScoreParameters {

  def createDescription(numberRecentlyJoinedIndividualCustomers: Int, thresholdDate: Date,
      numberRecentlyJoinedIndividualCustomersThreshold: Int): String = {
    s"There are $numberRecentlyJoinedIndividualCustomers customers who joined after $thresholdDate on the network" +
      s"(minimum inclusive limit for scenario is $numberRecentlyJoinedIndividualCustomersThreshold) "
  }

  val id = "HighNumberOfRecentlyJoinedIndividualCustomersOnNetwork"

  val parameters: Set[ScoreParameterIdentifier] = Set(
    ParameterIdentifier(None, "NumberRecentlyJoinedIndividualCustomersThreshold"),
    ParameterIdentifier(None, "ThresholdJoinDate")
  )

  def score(liteGraph: LiteGraphWithId)(implicit scoreInput: ScoreInput): Option[BasicScoreOutput] = {

    val numberRecentlyJoinedIndividualCustomersThreshold = parameter[Int]("NumberRecentlyJoinedIndividualCustomersThreshold")
    val joinDateThreshold = DateParsing.parseDate(parameter[String]("ThresholdJoinDate"))

    val numRecentlyJoinedIndividuals = liteGraph.graph.getEntitiesFromLiteGraphWithAttributes("individual")
      .filter(ent => isRecentlyJoinedCustomer(ent, joinDateThreshold)).distinct.size

    if (numRecentlyJoinedIndividuals >= numberRecentlyJoinedIndividualCustomersThreshold) {
      Some(BasicScoreOutput(
        description = createDescription(numRecentlyJoinedIndividuals, joinDateThreshold, numberRecentlyJoinedIndividualCustomersThreshold),
        severity = 100
      ))
    } else None
  }

  private def isRecentlyJoinedCustomer(entity: LiteEntity, thresholdDate: Date): Boolean = {
    val rawJoinedDate = getAttributeValue[Date]("customerStartDate", entity.attributes)
    rawJoinedDate.exists(_.after(thresholdDate))
  }

}