package com.quantexa.example.scoring.scores.fiu.propertytest

import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper.{dateGen, javaSqlDateGenerator}
import com.quantexa.example.scoring.scores.fiu.document.customer.NewCustomer
import com.quantexa.example.scoring.scores.fiu.testdata.DocumentTestData.testCustomer
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{DateScoreParameter, ParameterIdentifier}
import org.joda.time.{DateTime, Period}
import org.scalacheck.Prop.forAll
import org.scalatest.FlatSpec
import org.scalatest.prop.Checkers

class NewCustomerPropertyTest extends FlatSpec with Checkers {

  implicit val ScoreInputWithParameter = ScoreInput.empty.copy(parameters = Map(
    ParameterIdentifier(None, "NewCustomer_newDateThreshold") -> DateScoreParameter("NewCustomer_newDateThreshold", "test", java.sql.Date.valueOf("2019-01-01")))
  )

  "NewCustomerPropertyTest" should "not score dates before newDateThreshold" in {

    val earlyStartDate = new DateTime(1900, 1, 1, 0, 0)
    val earlyDateRange = Period.years(119)

    val property = forAll(javaSqlDateGenerator(earlyStartDate,earlyDateRange)) { date =>
      NewCustomer.score(testCustomer.copy(customerStartDate = Some(date))).isEmpty
    }
    check(property)
  }

  it should "score dates after newDateThreshold" in {
    val dateAfterThreshold = java.sql.Date.valueOf("2019-01-02")

    val property = forAll(dateGen(Some(dateAfterThreshold))) { date =>
      NewCustomer.score(testCustomer.copy(customerStartDate = Some(date))).flatMap(_.severity).get == 100
    }
    check(property)
  }

}