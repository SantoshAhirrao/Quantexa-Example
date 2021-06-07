package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.model.fiu.FactsModel.TransactionFacts
import com.quantexa.example.scoring.model.fiu.RollupModel.TransactionScoreOutput
import com.quantexa.example.scoring.utils.{Beneficiary, Counterparty, Customer, Originator, OriginatorOrBeneficiary}
import com.quantexa.example.scoring.utils.ScoringUtils.getCountry
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, HitAggregation}

object TxnFromLowToHighRiskCountry extends AugmentedDocumentScore[TransactionFacts, TransactionScoreOutput] with HitAggregation{

  val id = "TxnFromLowToHighRiskCountry"

  val name = "Transaction from low to high risk country"

  def createDescription(originCountry: String, destinationCountry: String): String = {
    s"Transaction from low risk country $originCountry to high risk country $destinationCountry."
  }

  override def dependencies = Set(CustomerAccountCountryRisk, CounterpartyAccountCountryRisk)

  def score(transaction: TransactionFacts)(implicit scoreInput: ScoreInput): Option[TransactionScoreOutput] = {
    val custCountry = getCountry(Customer, transaction)
    val cpCountry = getCountry(Counterparty, transaction)

    if ((transaction.customerOriginatorOrBeneficiary == "originator" || transaction.customerOriginatorOrBeneficiary == "beneficiary") &&
      custCountry.isDefined && cpCountry.isDefined) {
      val customerCountry = custCountry.get
      val counterpartyCountry = cpCountry.get

      val origOrBene: OriginatorOrBeneficiary = transaction.customerOriginatorOrBeneficiary match {
        case "originator" => Originator
        case "beneficiary" => Beneficiary
      }


      val (originCountry, originIsLowRisk): (String, Boolean) = origOrBene match {
        case Originator => (customerCountry, !scoreInput.previousScoreOutput.exists(_._1 == CustomerAccountCountryRisk)) //The country risk scores do not trigger unless the risk is medium or high - we infer it is low by the fact the score didn't trigger
        case Beneficiary => (counterpartyCountry, !scoreInput.previousScoreOutput.exists(_._1 == CounterpartyAccountCountryRisk)) //The country risk scores do not trigger unless the risk is medium or high - we infer it is low by the fact the score didn't trigger
      }

      val (destinationCountry, destinationIsHighRisk): (String, Boolean) = origOrBene match {
        case Beneficiary => (customerCountry, scoreInput.previousScoreOutput.filter(_._1 == CustomerAccountCountryRisk).exists(_._2.band.contains("high")))
        case Originator => (counterpartyCountry, scoreInput.previousScoreOutput.filter(_._1 == CounterpartyAccountCountryRisk).exists(_._2.band.contains("high")))
      }

      if (originIsLowRisk && destinationIsHighRisk) {
        Some(TransactionScoreOutput(keys = transaction.transactionKeys,
          band = None,
          description = Some(createDescription(originCountry, destinationCountry)),
          severity = Some(100)))
      } else None
    } else None
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}
