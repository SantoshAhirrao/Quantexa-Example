package com.quantexa.example.scoring.scores.fiu.document.customer

import com.quantexa.analytics.scala.scoring.ScoringUtils.severityFromContinuousValue
import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.model.fiu.customer.scoring.ScoreableCustomer
import com.quantexa.example.scoring.model.fiu.ScoringModel.{CustomerScoreOutput, CustomerScoreOutputKeys}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, HitAggregation}

/*
This score is here to show that usually continuous input variables should be mapped to continuous severities.
However customerRiskRating is not a good example of a continuous variable as there is not a clear mapping as
to how the risk ratings work. This score will remain in project-example as an example of a continuous mapping
but will not be included in the scorecard.
 */
object HighRiskCustomerContinuous extends AugmentedDocumentScore[Customer, CustomerScoreOutput]
  with HitAggregation {

  val id = "HighRiskCustomerContinuous"

  val name = "High Risk Customer Continuous"

  def score(customer: Customer)(implicit scoreInput: ScoreInput): Option[CustomerScoreOutput] = {
    score(ScoreableCustomer(customer))
  }

  def score(customer: ScoreableCustomer)(implicit scoreInput: ScoreInput): Option[CustomerScoreOutput] = {

    def createDescription(customerName: Option[String], customerId: Long): String = {
      if(customerName.isEmpty) s"High risk customer ($customerId)."
      else
        s"High risk customer ${customerName.get} ($customerId)."
    }

    val customerRisk = customer.customerRiskRating

    val severityValue = Some(severityFromContinuousValue(customerRisk.getOrElse(0).toDouble,6202,9018.6,20))

    val severity = severityValue.flatMap {
      case risk if risk > 0 => Some(risk)
      case _ => None
    }

    severity map {sev =>
      CustomerScoreOutput(
        keys = CustomerScoreOutputKeys(customer.customerIdNumberString),
        band = None,
        description = Some(createDescription(customer.fullName, customer.customerIdNumber)),
        severity = Some(sev)
      )
    }
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max
}
