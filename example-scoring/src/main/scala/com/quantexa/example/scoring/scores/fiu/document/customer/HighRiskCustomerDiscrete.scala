package com.quantexa.example.scoring.scores.fiu.document.customer

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.model.fiu.customer.scoring.ScoreableCustomer
import com.quantexa.example.scoring.model.fiu.ScoringModel.{CustomerScoreOutput, CustomerScoreOutputKeys}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, HitAggregation}

/*
Usually continuous input variables should be mapped to continuous severities. However customerRiskRating is
not a good example of a continuous variable as there is not a clear mapping for the risk ratings. Therefore
we have a discrete and continuous version of this score, where the discrete score is a better measure of overall
customer risk and the continuous score is a good example of a continuous severity mapping.
 */
object HighRiskCustomerDiscrete extends AugmentedDocumentScore[Customer, CustomerScoreOutput]
  with HitAggregation {

  val id = "HighRiskCustomerDiscrete"

  val name = "High Risk Customer Discrete"

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

    val severity = customerRisk.flatMap {
      case risk if risk > 9995 => Some(100)
      case risk if risk > 8998 => Some(80)
      case risk if risk > 8001 => Some(60)
      case risk if risk > 7004 => Some(40)
      case risk if risk > 6007 => Some(20)
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



