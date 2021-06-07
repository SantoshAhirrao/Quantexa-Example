package com.quantexa.example.scoring.scores.fiu.rollup

import com.quantexa.analytics.scala.scoring.scoretypes.{AugmentedDocumentScore, AugmentedDocumentScoreWithLookup}
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerRollup, CustomerScoreOutputWithUnderlying, TransactionScoreOutput}
import com.quantexa.example.scoring.model.fiu.ScoringModel.CustomerKey
import com.quantexa.example.scoring.scores.fiu.transaction.TxnFromHighToLowRiskCountry
import com.quantexa.example.scoring.utils.ScoringUtils.checkIfDateInLookbackPeriod
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, HitAggregation, LookupParameters}
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, ScoreParameterIdentifier, ScoreParameters}

case class CustomerRollupHighToLowRiskCountryBatch(runDate: java.sql.Date)
  extends AugmentedDocumentScore[CustomerRollup[TransactionScoreOutput], CustomerScoreOutputWithUnderlying]
    with CustomerRollupHighToLowRiskCountryTemplate

//When adding more types of rollup scores, it may be worth abstracting over the common elements
case class CustomerRollupHighToLowRiskCountryOnDemand(runDate: java.sql.Date)
  extends AugmentedDocumentScoreWithLookup[Customer, CustomerRollup[TransactionScoreOutput], CustomerScoreOutputWithUnderlying]
    with CustomerRollupHighToLowRiskCountryTemplate with HitAggregation {

  def lookupParams: LookupParameters[Customer] = LookupParameters(x => x.customerIdNumberString, None)

  def score(subject: Customer, lookup: CustomerRollup[TransactionScoreOutput])(implicit scoreInput: ScoreInput): Option[CustomerScoreOutputWithUnderlying] = {

    this.score(lookup)
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}


trait CustomerRollupHighToLowRiskCountryTemplate extends ScoreParameters{

  def runDate: java.sql.Date

  def id: String = "CR101_CustomerRollupHighToLowRiskCountry"
  def name: String = "Customer has transaction(s) from a high risk country to a low risk country"

  def score(document: CustomerRollup[TransactionScoreOutput])(implicit scoreInput: ScoreModel.ScoreInput): Option[CustomerScoreOutputWithUnderlying] = {
    val outcomePeriodMonths = parameter[Int]("outcomePeriodMonths")
    val triggeringScores = document.customScoreOutputMap
      .getOrElse(TxnFromHighToLowRiskCountry.id, Seq.empty).
      filter(score => checkIfDateInLookbackPeriod(score.keys.analysisDate, runDate, outcomePeriodMonths))

    if (triggeringScores.isEmpty) None
    else {
      val topScoreBySeverity = triggeringScores.maxBy(_.severity)
      Some(CustomerScoreOutputWithUnderlying(
        keys = CustomerKey(document.keys.customerId),
        severity = topScoreBySeverity.severity,
        band = topScoreBySeverity.band,
        description = Some(s"The customer has ${triggeringScores.size} transactions which were from high to low risk countries in the past $outcomePeriodMonths months"),
        underlyingScores = triggeringScores.map(_.toKeyedBasicScoreOutput)
      )
      )
    }
  }

  def parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(namespace = None, name = "outcomePeriodMonths"))
}