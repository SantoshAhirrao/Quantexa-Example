package com.quantexa.example.etl.projects.fiu

import java.sql.Date

import com.quantexa.example.etl.projects.fiu.utils.ProjectParsingUtils.parseDate
import com.quantexa.model.core.datasets.ParsedDatasets.ParsedDateParts
import org.scalatest.{FlatSpec, Matchers}

/***
  * This class was created to show an example unit test for a function
  */

class CleanseCaseClassTest extends FlatSpec with Matchers {
  "CleanseCaseClass.parseDoB" should "Parse every month correctly" in {

    val input = Seq(
      Date.valueOf("2016-01-01")
      , Date.valueOf("2016-02-01")
      , Date.valueOf("2016-03-01")
      , Date.valueOf("2016-04-01")
      , Date.valueOf("2016-05-01")
      , Date.valueOf("2016-06-01")
      , Date.valueOf("2016-07-01")
      , Date.valueOf("2016-08-01")
      , Date.valueOf("2016-09-01")
      , Date.valueOf("2016-10-01")
      , Date.valueOf("2016-11-01")
      , Date.valueOf("2016-12-01")
    )

    val expected = Seq(
      Some(ParsedDateParts(Some(Date.valueOf("2016-01-01")),Some("2016"),Some("01"),Some("01")))
      , Some(ParsedDateParts(Some(Date.valueOf("2016-02-01")),Some("2016"),Some("02"),Some("01")))
      , Some(ParsedDateParts(Some(Date.valueOf("2016-03-01")),Some("2016"),Some("03"),Some("01")))
      , Some(ParsedDateParts(Some(Date.valueOf("2016-04-01")),Some("2016"),Some("04"),Some("01")))
      , Some(ParsedDateParts(Some(Date.valueOf("2016-05-01")),Some("2016"),Some("05"),Some("01")))
      , Some(ParsedDateParts(Some(Date.valueOf("2016-06-01")),Some("2016"),Some("06"),Some("01")))
      , Some(ParsedDateParts(Some(Date.valueOf("2016-07-01")),Some("2016"),Some("07"),Some("01")))
      , Some(ParsedDateParts(Some(Date.valueOf("2016-08-01")),Some("2016"),Some("08"),Some("01")))
      , Some(ParsedDateParts(Some(Date.valueOf("2016-09-01")),Some("2016"),Some("09"),Some("01")))
      , Some(ParsedDateParts(Some(Date.valueOf("2016-10-01")),Some("2016"),Some("10"),Some("01")))
      , Some(ParsedDateParts(Some(Date.valueOf("2016-11-01")),Some("2016"),Some("11"),Some("01")))
      , Some(ParsedDateParts(Some(Date.valueOf("2016-12-01")),Some("2016"),Some("12"),Some("01")))
    )

    val result = input.map(date => parseDate(Some(date)))
    result should contain theSameElementsAs expected
  }
}
