package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.model.fiu.FactsModel.TransactionFacts
import com.quantexa.example.scoring.model.fiu.RollupModel.TransactionScoreOutput
import com.quantexa.example.scoring.utils.ScoringUtils.getCountry
import com.quantexa.example.scoring.utils.{Counterparty, Customer, CustomerOrCounterparty}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, HitAggregation}
import com.quantexa.scoring.framework.parameters.{ScoreParameterIdentifier, ParameterIdentifier, ScoreParameters}

trait AccountCountryRisk extends AugmentedDocumentScore[TransactionFacts, TransactionScoreOutput]
  with ScoreParameters with HitAggregation {

  def id: String

  def name: String

  def customerOrCounterParty: CustomerOrCounterparty

  val parameters: Set[ScoreParameterIdentifier] = Set(
    ParameterIdentifier(None, "MediumRiskThresholdCorruptionScore"),
    ParameterIdentifier(None, "HighRiskThresholdCorruptionScore")
  )

  def createDescription(Country: String, level: String): String = {
    s"$Country is a $level risk country."
  }

  def score(transaction: TransactionFacts)(implicit scoreInput: ScoreInput): Option[TransactionScoreOutput] = {

    val mediumRiskThreshold = parameter[Int]("MediumRiskThresholdCorruptionScore")
    val highRiskThreshold = parameter[Int]("HighRiskThresholdCorruptionScore")

    val country = getCountry(customerOrCounterParty, transaction)

    country.flatMap {
      ctry =>
        val corruptionLevel = customerOrCounterParty match {
          case Customer => transaction.customerAccountCountryCorruptionIndex
          case Counterparty => transaction.counterPartyAccountCountryCorruptionIndex
        }

        corruptionLevel.flatMap { risk =>
          val transactionKeys = transaction.transactionKeys
          risk match {
            case risk if risk < highRiskThreshold => Some(TransactionScoreOutput(
              keys = transactionKeys,
              severity = Some(100),
              band = Some("high"),
              description = Some(createDescription(ctry, "high"))))
            case risk if risk < mediumRiskThreshold => Some(TransactionScoreOutput(
              keys = transactionKeys,
              severity = Some(50),
              band = Some("medium"),
              description = Some(createDescription(ctry, "medium"))))
            case _ => None
          }
        }
    }
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}

object CustomerAccountCountryRisk extends AccountCountryRisk{
  def id = "CustomerCountryRisk"
  def name = "Customer country risk"
  def customerOrCounterParty = Customer
}

object CounterpartyAccountCountryRisk extends AccountCountryRisk{
  def id = "CounterpartyCountryRisk"
  def name = "Counterparty country risk"
  def customerOrCounterParty = Counterparty
}

