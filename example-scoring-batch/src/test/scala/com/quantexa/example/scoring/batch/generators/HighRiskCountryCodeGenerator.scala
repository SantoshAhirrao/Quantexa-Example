package com.quantexa.example.scoring.batch.generators

import com.quantexa.example.model.fiu.lookupdatamodels.HighRiskCountryCode
import org.scalacheck.Gen

import scala.reflect.ClassTag

case object HighRiskCountryCodeGenerator extends GeneratesDataset[HighRiskCountryCode] {

  val highRiskCountryCodes = Seq(
    HighRiskCountryCode("ZA"),
    HighRiskCountryCode("PL"),
    HighRiskCountryCode("HU"))


  def generate(implicit ct: ClassTag[HighRiskCountryCode]): Gen[HighRiskCountryCode] =
    Gen.oneOf(highRiskCountryCodes)

}
