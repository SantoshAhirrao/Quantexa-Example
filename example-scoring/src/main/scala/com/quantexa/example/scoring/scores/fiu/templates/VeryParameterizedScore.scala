package com.quantexa.example.scoring.scores.fiu.templates

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.utils.ScoringUtils.getCountry
import com.quantexa.example.scoring.utils.{Counterparty, Customer, CustomerOrCounterparty}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{ScoreParameterIdentifier, ParameterIdentifier, ScoreParameters}
import com.quantexa.analytics.scala.scoring._
import com.quantexa.example.scoring.model.fiu.FactsModel.TransactionFacts
import com.quantexa.example.scoring.model.fiu.RollupModel.TransactionScoreOutput

trait VeryParameterizedScore extends AugmentedDocumentScore[TransactionFacts, TransactionScoreOutput] with ScoreParameters {

  def id: String

  def name: String

  def customerOrCounterParty: CustomerOrCounterparty

  val parameters: Set[ScoreParameterIdentifier] = Set(
    ParameterIdentifier(None, "CountryCorruption_MediumRiskThreshold"),
    ParameterIdentifier(None, "CountryCorruption_HighRiskThreshold"),
    ParameterIdentifier(None, "AccountCountryRisk_MediumRiskSeverity"),
    ParameterIdentifier(None, "AccountCountryRisk_HighRiskSeverity"),
    ParameterIdentifier(None, "MediumRisk_DescriptionString"),
    ParameterIdentifier(None, "HighRisk_DescriptionString"),
    ParameterIdentifier(None, "AccountCountryRisk_Description"),
    ParameterIdentifier(None, "Customer_DescriptionString"),
    ParameterIdentifier(None, "Counterparty_DescriptionString"))

  private case class SeverityBandAndDescriptionData(severity: Int,
                                                     riskLevel: String,
                                                     band: String)

  def score(transaction: TransactionFacts)(implicit scoreInput: ScoreInput): Option[TransactionScoreOutput] = {

    val mediumRiskThreshold = parameter[Int]("CountryCorruption_MediumRiskThreshold")
    val highRiskThreshold = parameter[Int]("CountryCorruption_HighRiskThreshold")
    val mediumRiskSeverity = parameter[Int]("AccountCountryRisk_MediumRiskSeverity")
    val highRiskSeverity = parameter[Int]("AccountCountryRisk_HighRiskSeverity")
    val mediumRiskString = parameter[String]("MediumRisk_DescriptionString")
    val highRiskString = parameter[String]("HighRisk_DescriptionString")
    val highRiskCountryDescription = parameter[String]("AccountCountryRisk_Description")

    val country = getCountry(customerOrCounterParty, transaction)

    val (corruptionLevel, custOrCpty) = customerOrCounterParty match {
      case Customer => (transaction.customerAccountCountryCorruptionIndex, parameter[String]("Customer_DescriptionString"))
      case Counterparty => (transaction.counterPartyAccountCountryCorruptionIndex, parameter[String]("Counterparty_DescriptionString"))
    }

    country.flatMap{
      ctry =>
        val accountInformation = if (corruptionLevel.getOrElse(0) > highRiskThreshold) {
          Some(SeverityBandAndDescriptionData(highRiskSeverity, highRiskString, "high"))
        } else if (corruptionLevel.getOrElse(0) > mediumRiskThreshold) {
          Some(SeverityBandAndDescriptionData(mediumRiskSeverity, mediumRiskString, "medium"))
        } else None

        accountInformation.map { x =>
          TransactionScoreOutput(
            keys = transaction.transactionKeys,
            severity = Some(x.severity),
            band = Some(x.band),
            description = Some(ScoringUtils.scoreDescriptionFromParameters(highRiskCountryDescription,
              Map("customerOrCounterparty" -> custOrCpty, "descriptionCountry" -> ctry, "riskLevel" -> x.riskLevel))))

        }
    }
  }
}

object CustomerAccountCountryRisk extends VeryParameterizedScore {
  def id = "CustomerCountryRisk"

  def name = "Customer Country Risk"

  def customerOrCounterParty = Customer
}

object CounterpartyAccountCountryRisk extends VeryParameterizedScore {
  def id = "CounterpartyCountryRisk"

  def name = "Counterparty Country Risk"

  def customerOrCounterParty = Counterparty
}

