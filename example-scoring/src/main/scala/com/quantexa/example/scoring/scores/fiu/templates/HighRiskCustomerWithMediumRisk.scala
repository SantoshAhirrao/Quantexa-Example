

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.model.fiu.ScoringModel.{ CustomerScoreOutput, CustomerScoreOutputKeys }
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters._

import scala.util.Try

object HighRiskCustomerWithMediumRisk extends AugmentedDocumentScore[Customer, CustomerScoreOutput]
    with ScoreParameters {

  val id = "HighRiskCustomerWithMediumRisk"
  def name = "High Risk Customer With Medium Risk"
  private case class SeverityAndRiskLevel(severity: Int, riskLevel: String)

  val parameters: Set[ScoreParameterIdentifier] = Set(
    ParameterIdentifier(None, "HighRiskCustomer_HighRiskRatingThreshold"),
    ParameterIdentifier(None, "HighRiskCustomer_MediumRiskRatingThreshold"),
    ParameterIdentifier(None, "MediumRisk_DescriptionString"),
    ParameterIdentifier(None, "HighRisk_DescriptionString"),
    ParameterIdentifier(None, "HighRiskCustomer_HighSeverityRating"),
    ParameterIdentifier(None, "HighRiskCustomer_MediumSeverityRating"))

  def score(customer: Customer)(implicit scoreInput: ScoreInput): Option[CustomerScoreOutput] = {
    val highRiskRatingThreshold = parameter[Int]("HighRiskCustomer_HighRiskRatingThreshold")
    val mediumRiskRatingThreshold = parameter[Int]("HighRiskCustomer_MediumRiskRatingThreshold")

    val customerRisk = customer.customerRiskRating.getOrElse(0)

    val severityAndRiskText = if (customerRisk > highRiskRatingThreshold) {
      Some(SeverityAndRiskLevel(parameter[Int]("HighRiskCustomer_HighSeverityRating"),
        parameter[String]("HighRisk_DescriptionString")))
    } else if (customerRisk > mediumRiskRatingThreshold) Some(SeverityAndRiskLevel(
      parameter[Int]("HighRiskCustomer_MediumSeverityRating"),
      parameter[String]("MediumRisk_DescriptionString")))
    else None

    severityAndRiskText.map { x =>

      CustomerScoreOutput(
        keys = CustomerScoreOutputKeys(customer.customerIdNumberString),
        description = Some(s"Customer has a ${x.riskLevel} risk rating: ${customerRisk}"),
        severity = Some(x.severity))

    }
  }
}