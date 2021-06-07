package com.quantexa.example.scoring.scores.fiu.propertytest

import com.quantexa.example.scoring.scores.fiu.document.customer.HighRiskCustomerContinuous
import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.testCustomer
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatest.FlatSpec
import org.scalatest.prop.Checkers

class HighRiskCustomerContinuousPropertyTest extends FlatSpec with Checkers {

  implicit val scoreInput = ScoreInput.empty

   "HighRiskCustomerContinuousPropertyTest" should "not score customers below min risk threshold" in {
    val lowRiskGen = Gen.chooseNum(0, 6201)

    val property = forAll(lowRiskGen) { risk =>
      HighRiskCustomerContinuous.score(testCustomer.copy(customerRiskRating = Some(risk))).isEmpty
    }
    check(property)
  }

  it should "score customers above risk threshold" in {
    val midRiskGen = Gen.chooseNum(6202, 10000)

    val property = forAll(midRiskGen, minSuccessful(1)) { (risk, _) =>
      val severityResult = HighRiskCustomerContinuous.score(testCustomer.copy(customerRiskRating = Some(risk))).flatMap(_.severity).getOrElse(0)
      severityResult <= 100 && severityResult >=20
    }

    check(property)
  }

  it should "order severities by customer risk" in {
    val orderedRiskGen = for {
      n <- Gen.chooseNum(0,10000)
      m <- Gen.choose(n, 10000)
    } yield (n,m)

    val property = forAll(orderedRiskGen) { case(lowerRisk, higherRisk) =>
      val severityLow = HighRiskCustomerContinuous.score(testCustomer.copy(customerRiskRating = Some(lowerRisk))).flatMap(_.severity).getOrElse(0)
      val severityHigh = HighRiskCustomerContinuous.score(testCustomer.copy(customerRiskRating = Some(higherRisk))).flatMap(_.severity).getOrElse(0)
      severityLow <= severityHigh
    }
    check(property)
  }

}