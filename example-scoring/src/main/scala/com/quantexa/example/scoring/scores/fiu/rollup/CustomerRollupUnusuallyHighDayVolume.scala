package com.quantexa.example.scoring.scores.fiu.rollup

import com.quantexa.analytics.scala.scoring.model.ScoringModel.ScoreId
import com.quantexa.analytics.scala.scoring.scoretypes.{AugmentedDocumentScore, AugmentedDocumentScoreWithLookup}
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerDateScoreOutput, CustomerRollup, CustomerScoreOutputWithUnderlying}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, HitAggregation, LookupParameters}
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, ScoreParameterIdentifier, ScoreParameters}
import com.quantexa.example.scoring.scores.fiu.transaction.{UnusuallyHigh1DayVolume, UnusuallyHigh30DaysVolume, UnusuallyHigh7DaysVolume}
import com.quantexa.example.scoring.utils.ScoringUtils.checkIfDateInLookbackPeriod
import com.quantexa.scoring.framework.model.ScoreModel

case class CustomerRollupUnusuallyHighDayVolumeBatch(runDate: java.sql.Date)
  extends AugmentedDocumentScore[CustomerRollup[CustomerDateScoreOutput], CustomerScoreOutputWithUnderlying]
    with CustomerRollupUnusuallyHighDayVolumeTemplate


//When adding more types of rollup scores, it may be worth abstracting over the common elements
case class CustomerRollupUnusuallyHighDayVolumeOnDemand(runDate: java.sql.Date)
  extends AugmentedDocumentScoreWithLookup[Customer, CustomerRollup[CustomerDateScoreOutput], CustomerScoreOutputWithUnderlying]
    with CustomerRollupUnusuallyHighDayVolumeTemplate with HitAggregation {

  def lookupParams: LookupParameters[Customer] = LookupParameters(x => x.customerIdNumberString, None)

  def score(subject: Customer, lookup: CustomerRollup[CustomerDateScoreOutput])(implicit scoreInput: ScoreInput): Option[CustomerScoreOutputWithUnderlying] = {

    this.score(lookup)
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}


trait CustomerRollupUnusuallyHighDayVolumeTemplate extends ScoreParameters{

  def runDate: java.sql.Date
  def id: String = "CR103_CustomerRollupUnusuallyHighDayVolume"
  def name: String = "Customer Spikes in Transaction Volume rollup"

  def parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(namespace = None, name = "outcomePeriodMonths"))

  def score(document: CustomerRollup[CustomerDateScoreOutput])(implicit scoreInput: ScoreModel.ScoreInput): Option[CustomerScoreOutputWithUnderlying] = {

    val outcomePeriodMonths = parameter[Int]("outcomePeriodMonths")
    val relevantScores = Set(UnusuallyHigh1DayVolume.id, UnusuallyHigh7DaysVolume.id, UnusuallyHigh30DaysVolume.id)
    val relevantTriggeredScores = document.customScoreOutputMap.
      filterKeys(scoreId => relevantScores(scoreId)).
      mapValues(_.filter(score => checkIfDateInLookbackPeriod(score.keys.analysisDate, runDate, outcomePeriodMonths)))

    val updateTriggeredScoreSeverities = (groupScoresByDate _ andThen modifySeverity)(relevantTriggeredScores)
    val regroupedBySeverity = updateTriggeredScoreSeverities.groupBy(_._2.severity)

    if (regroupedBySeverity.isEmpty) None
    else {
      val topScoreBySeverityAndLookbackPeriod = relevantScores.toSeq.flatMap(regroupedBySeverity.maxBy(_._1)._2.toMap.get).headOption

      topScoreBySeverityAndLookbackPeriod.map { customerDateScoreOutput =>
        CustomerScoreOutputWithUnderlying(
          keys = document.keys,
          severity = customerDateScoreOutput.severity,
          band = customerDateScoreOutput.band,
          description = customerDateScoreOutput.description.map { scoreDesc =>
            s"In the last $outcomePeriodMonths months, the greatest unusual volume occurred on ${customerDateScoreOutput.keys.analysisDate} where; $scoreDesc"
          },
          underlyingScores = relevantTriggeredScores.values.flatten.map(_.toKeyedBasicScoreOutput).toSeq)
      }
    }
  }

  private def groupScoresByDate(relevantTriggeredScores:collection.Map[ScoreId, Seq[CustomerDateScoreOutput]]) : Map[java.sql.Date, Seq[(ScoreId, CustomerDateScoreOutput)]] = {
    val scoreIdToScoreOutput = relevantTriggeredScores.toSeq.flatMap { case (scoreId, scoreOutputs) =>
      scoreOutputs.map(scoreId -> _)
    }

    scoreIdToScoreOutput.groupBy(_._2.keys.analysisDate)
  }

  private def modifySeverity(dateGroupedTriggeredScores: Map[java.sql.Date, Seq[(ScoreId, CustomerDateScoreOutput)]]): Seq[(ScoreId, CustomerDateScoreOutput)] = {
    dateGroupedTriggeredScores.map {
      case (_, scoresTriggeredWithId) =>
        val severityMultiplier = scoresTriggeredWithId.size match {
          case 2 if Set(UnusuallyHigh1DayVolume.id, UnusuallyHigh7DaysVolume.id).subsetOf(scoresTriggeredWithId.toMap.keySet) => 0.76
          case 2 => 0.53
          case 1 => 0.3
          case 0 => 0 //Handle 0 for completeness
          case _ => 1
        }
        scoresTriggeredWithId.toMap.mapValues{ cdso => cdso.copy(severity = cdso.severity.map(sev => (sev * severityMultiplier).toInt))}
    }.toSeq.flatten
  }
}