package com.quantexa.example.scoring.scores.fiu.document.customer

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.model.fiu.ScoringModel.{CustomerScoreOutput, CustomerScoreOutputKeys}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.model.scores.{AggregationFunction, HitAggregation, AggregationFunctions}


/**Triggers if the customers status indicates they have been cancelled*/
object CancelledCustomer extends AugmentedDocumentScore[Customer, CustomerScoreOutput]
  with HitAggregation {

  def id = "CancelledCustomer"

  def name: String = "Cancelled Customer"

  def score(customer: Customer)(implicit scoreInput: ScoreInput): Option[CustomerScoreOutput] = {
    if (customer.customerStatus.contains("C")) Some(
      CustomerScoreOutput(
        keys = CustomerScoreOutputKeys(customer.customerIdNumberString),
        description = Some(s"Customer ${customer.fullName.map(_ + "'s ").getOrElse("")}(${customer.customerIdNumber}) accounts with the bank have been cancelled by the investigations team."),
        severity = Some(100)
      )
    ) else None
  }

  override def aggregationFunction: AggregationFunction = AggregationFunctions.Max

}
