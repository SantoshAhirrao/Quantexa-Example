package com.quantexa.example.scoring.scores.fiu.rollup

import com.quantexa.analytics.scala.scoring.scoretypes.{AugmentedDocumentScore, AugmentedDocumentScoreWithLookup}
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerRollup, CustomerScoreOutputWithUnderlying, TransactionScoreOutput}
import com.quantexa.example.scoring.scores.fiu.transaction.UnusualBeneficiaryCountryForCustomer
import com.quantexa.example.scoring.utils.ScoringUtils.{checkIfDateInLookbackPeriod, sortByValueAndByKey}
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, HitAggregation, LookupParameters}
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, ScoreParameterIdentifier, ScoreParameters}

case class CustomerRollupUnusualBeneficiaryCountryForCustomerBatch(runDate: java.sql.Date)
  extends AugmentedDocumentScore[CustomerRollup[TransactionScoreOutput], CustomerScoreOutputWithUnderlying]
    with CustomerRollupUnusualBeneficiaryCountryForCustomerTemplate


case class CustomerRollupUnusualBeneficiaryCountryForCustomerOnDemand(runDate: java.sql.Date)
  extends AugmentedDocumentScoreWithLookup[Customer, CustomerRollup[TransactionScoreOutput], CustomerScoreOutputWithUnderlying]
    with CustomerRollupUnusualBeneficiaryCountryForCustomerTemplate with HitAggregation {

  def lookupParams: LookupParameters[Customer] = LookupParameters(x => x.customerIdNumberString, None)

  def score(subject: Customer, lookup: CustomerRollup[TransactionScoreOutput])(implicit scoreInput: ScoreInput): Option[CustomerScoreOutputWithUnderlying] = {

    this.score(lookup)
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}


trait CustomerRollupUnusualBeneficiaryCountryForCustomerTemplate extends ScoreParameters{

  def runDate: java.sql.Date
  def id: String = "CR104_CustomerRollupUnusualBeneficiaryCountryForCustomer"
  def name: String = "Unusual beneficiary for customer"

  def parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(namespace = None, name = "outcomePeriodMonths"),
    ParameterIdentifier(namespace = None, name = "CustomerRollupUnusualCountryScores_maxNumberOfCountriesInDescription"))

  def score(document: CustomerRollup[TransactionScoreOutput])(implicit scoreInput: ScoreModel.ScoreInput): Option[CustomerScoreOutputWithUnderlying] = {
    val outcomePeriodMonths = parameter[Int]("outcomePeriodMonths")
    val maxNumberOfCountriesInDescription = parameter[Int]("CustomerRollupUnusualCountryScores_maxNumberOfCountriesInDescription")

    val triggeringScores: Seq[TransactionScoreOutput] = document.customScoreOutputMap.getOrElse(UnusualBeneficiaryCountryForCustomer.id, Seq.empty)
      .filter(score => checkIfDateInLookbackPeriod(score.keys.analysisDate, runDate, outcomePeriodMonths))

    if (triggeringScores.isEmpty) None
    else {
      val topScoreBySeverity = triggeringScores.maxBy(_.severity)
      val unusualCountries = triggeringScores
        .flatMap(_.extraDescriptionText.get("country"))
        .groupBy(identity)
        .mapValues(_.size)
        .toSeq
        .sortWith( sortByValueAndByKey )
        .map(_._1)

      Some(CustomerScoreOutputWithUnderlying(
        keys = document.keys,
        severity = topScoreBySeverity.severity,
        band = topScoreBySeverity.band,
        description = Some(s"The customer has ${triggeringScores.size} transactions which were from ${unusualCountries.size} " +
          s"unusual beneficiary countries: ${unusualCountries.take(maxNumberOfCountriesInDescription).mkString(", ")} " +
          s"in the past $outcomePeriodMonths months"),
        underlyingScores = triggeringScores.map(_.toKeyedBasicScoreOutput)
      ))
    }
  }
}