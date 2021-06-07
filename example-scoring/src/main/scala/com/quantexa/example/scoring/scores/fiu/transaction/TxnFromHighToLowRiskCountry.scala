package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.model.fiu.FactsModel.TransactionFacts
import com.quantexa.example.scoring.model.fiu.RollupModel.TransactionScoreOutput
import com.quantexa.example.scoring.utils.{Beneficiary, Counterparty, Customer, Originator, OriginatorOrBeneficiary}
import com.quantexa.example.scoring.utils.ScoringUtils.getCountry
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, HitAggregation}

object TxnFromHighToLowRiskCountry extends AugmentedDocumentScore[TransactionFacts, TransactionScoreOutput]
  with HitAggregation {

  val id = "T101_TxnFromHighToLowRiskCountry"

  val name = "Transaction from high to low risk country"

  override def dependencies = Set(CustomerAccountCountryRisk, CounterpartyAccountCountryRisk)

  def createDescription(originCountry: String, destinationCountry: String): String = {
      s"Transaction from high risk country $originCountry to low risk country $destinationCountry."
  }

  def score(transaction: TransactionFacts)(implicit scoreInput: ScoreInput): Option[TransactionScoreOutput] = {
    val custCountry = getCountry(Customer, transaction)
    val cpCountry = getCountry(Counterparty, transaction)

    if((transaction.customerOriginatorOrBeneficiary == "originator" || transaction.customerOriginatorOrBeneficiary == "beneficiary") &&
      custCountry.isDefined && cpCountry.isDefined){
      val customerCountry = custCountry.get
      val counterpartyCountry = cpCountry.get

      val origOrBene: OriginatorOrBeneficiary = transaction.customerOriginatorOrBeneficiary match {
        case "originator" => Originator
        case "beneficiary" => Beneficiary
      }
      val (originCountry, originIsHighRisk): (String, Boolean) = origOrBene match {
        case Originator => (customerCountry, scoreInput.previousScoreOutput.filter(_._1 == CustomerAccountCountryRisk).exists(_._2.band.contains("high")))
        case Beneficiary => (counterpartyCountry, scoreInput.previousScoreOutput.filter(_._1 == CounterpartyAccountCountryRisk).exists(_._2.band.contains("high")))
      }

      val (destinationCountry, destinationIsLowRisk): (String, Boolean) = origOrBene match {
        case Beneficiary => (customerCountry, !scoreInput.previousScoreOutput.exists(_._1 == CustomerAccountCountryRisk)) //The country risk scores do not trigger unless the risk is medium or high - we infer it is low by the fact the score didn't trigger
        case Originator => (counterpartyCountry, !scoreInput.previousScoreOutput.exists(_._1 == CounterpartyAccountCountryRisk)) //The country risk scores do not trigger unless the risk is medium or high - we infer it is low by the fact the score didn't trigger
      }

      if (originIsHighRisk && destinationIsLowRisk) {
        Some(TransactionScoreOutput(keys = transaction.transactionKeys,
          severity = Some(100), band = None, description = Some(createDescription(originCountry, destinationCountry))))
      } else None
    } else None
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}
