package com.quantexa.example.model.fiu.customer.scoring

import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import io.scalaland.chimney.dsl._

case class ScoreableCustomer(
                              customerIdNumber: Long,
                              customerIdNumberString: String,
                              fullName: Option[String],
                              customerRiskRating: Option[Int]
                            )

object ScoreableCustomer{

  def apply(customer: Customer): ScoreableCustomer = customer.into[ScoreableCustomer].transform

}