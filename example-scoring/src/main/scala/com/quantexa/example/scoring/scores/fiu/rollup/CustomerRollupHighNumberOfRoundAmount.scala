package com.quantexa.example.scoring.scores.fiu.rollup

import com.quantexa.analytics.scala.scoring.scoretypes.{AugmentedDocumentScore, AugmentedDocumentScoreWithLookup}
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerRollup, CustomerScoreOutputWithUnderlying, TransactionScoreOutput}
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, ScoreParameterIdentifier, ScoreParameters}
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, HitAggregation, LookupParameters}
import com.quantexa.example.scoring.scores.fiu.transaction.TransactionIsRoundAmount
import com.quantexa.example.scoring.utils.ScoringUtils.checkIfDateInLookbackPeriod
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput

case class CustomerRollupHighNumberOfRoundAmountBatch(runDate: java.sql.Date)
  extends AugmentedDocumentScore[CustomerRollup[TransactionScoreOutput], CustomerScoreOutputWithUnderlying]
    with CustomerRollupHighNumberOfRoundAmountTemplate


//When adding more types of rollup scores, it may be worth abstracting over the common elements
case class CustomerRollupHighNumberOfRoundAmountOnDemand(runDate: java.sql.Date)
  extends AugmentedDocumentScoreWithLookup[Customer, CustomerRollup[TransactionScoreOutput], CustomerScoreOutputWithUnderlying]
  with CustomerRollupHighNumberOfRoundAmountTemplate with HitAggregation {

  def lookupParams: LookupParameters[Customer] = LookupParameters(x => x.customerIdNumberString, None)

  def score(subject: Customer, lookup: CustomerRollup[TransactionScoreOutput])(implicit scoreInput: ScoreInput): Option[CustomerScoreOutputWithUnderlying] = {

    this.score(lookup)
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}


trait CustomerRollupHighNumberOfRoundAmountTemplate
  extends ScoreParameters {

  def runDate: java.sql.Date

  def id: String = "CR100_CustomerRollupHighNumberOfRoundAmount"
  def name: String = "Customer has high number of round amount transactions rollup"

  def parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(namespace = None, name = "outcomePeriodMonths"))

  def score(document: CustomerRollup[TransactionScoreOutput])(implicit scoreInput: ScoreModel.ScoreInput): Option[CustomerScoreOutputWithUnderlying] = {
    val outcomePeriodMonths = parameter[Int]("outcomePeriodMonths")
    val triggeringScores = document.customScoreOutputMap
      .getOrElse(TransactionIsRoundAmount.id, Seq.empty).
      filter(score => checkIfDateInLookbackPeriod(score.keys.analysisDate, runDate, outcomePeriodMonths))

    if (triggeringScores.isEmpty) None
    else {
      val topScoreBySeverity = triggeringScores.maxBy(_.severity)
      Some(CustomerScoreOutputWithUnderlying(
        keys = document.keys,
        severity = topScoreBySeverity.severity,
        band = topScoreBySeverity.band,
        description = Some(s"The customer has ${triggeringScores.size} transactions which were round amounts in the last $outcomePeriodMonths months"),
        underlyingScores = triggeringScores.map(_.toKeyedBasicScoreOutput)
      )
      )
    }
  }
}