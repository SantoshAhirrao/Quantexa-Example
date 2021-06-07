package com.quantexa.example.model.fiu

import com.quantexa.scoring.framework.model.LookupModel.LookupSchema
import com.quantexa.scoring.framework.model.LookupModel.KeyAnnotation

package object lookupdatamodels {

  case class HighRiskCountryCode(@KeyAnnotation riskCountryCode: String) extends LookupSchema

  case class PostcodePrice(@KeyAnnotation postcode: String,
                            price: Double) extends LookupSchema
  
}