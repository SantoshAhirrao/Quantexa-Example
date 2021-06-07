package com.quantexa.example.etl.projects.fiu.transaction

import com.quantexa.example.model.fiu.transaction.TransactionModel.TransactionAccount
import org.scalatest.{FlatSpec, Matchers}

class CleanseCaseClassTest extends FlatSpec with Matchers {

  "cleanCountry" should "convert iso2 format to iso3" in {
    val iso2 = Some("AU")
    val expectedIso3 = Some("AUS")
    val trnAccount = TransactionAccount("1", "0123456789", None, None, None, None, None, iso2, None)

    val actualIso3 = CleanseCaseClass.cleanseCountry(trnAccount)

    actualIso3 should be(expectedIso3)
  }
}
