package com.quantexa.example.scoring.scores.fiu.document.customer

import scala.util.Try
import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.model.fiu.ScoringModel.{CustomerScoreOutput, CustomerScoreOutputKeys}
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, AggregationFunctions, HitAggregation}
import com.quantexa.scoring.framework.parameters.{ScoreParameters, ParameterIdentifier, ScoreParameterIdentifier}

object NewCustomer extends AugmentedDocumentScore[Customer,CustomerScoreOutput]
  with ScoreParameters with HitAggregation {
  val id = "NewCustomer"
  def name = "New Customer"

  def parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(None, "NewCustomer_newDateThreshold"))

  def score(customer: Customer)(implicit scoreInput: ScoreInput): Option[CustomerScoreOutput] = {
    val newDateThreshold = parameter[java.sql.Date]("NewCustomer_newDateThreshold")
    if (Try(customer.customerStartDate.getOrElse(newDateThreshold)).isSuccess) {
      if (customer.customerStartDate.getOrElse(newDateThreshold).after(newDateThreshold)){
        Some(
          CustomerScoreOutput(
            keys = CustomerScoreOutputKeys(customer.customerIdNumberString),
              description = Some(s"Customer ${customer.fullName.getOrElse("")}'s (${customer.customerIdNumber}) has a recent start date: ${customer.customerStartDate.get.toString}"),
              severity = Some(100)
          )
        )
      }
      else None
    }
    else None
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}