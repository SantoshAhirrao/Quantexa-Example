package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate


import com.quantexa.example.scoring.batch.tags.SparkTest
import com.quantexa.example.scoring.batch.scores.fiu.facts.{CalculateCustomerDateFacts, CustomerDateStatsConfiguration}
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.example.scoring.batch.scores.fiu.facts.CalculateCustomerDateFacts
import com.quantexa.example.scoring.batch.scores.fiu.DocumentTestData._
import org.apache.spark.sql.types._
import org.scalatest.{FlatSpec, Matchers}

@SparkTest
class CustomerDateFactsTests extends FlatSpec with Matchers{

  val custDateFacts = CalculateCustomerDateFacts(
    config = ProjectExampleConfig.default("/"),
    stats = CustomerDateStatsConfiguration.empty
  )

  "setColumnOrder" should "order input columns based on a suggested ordering" in {
    val orderedColumns = custDateFacts.setColumnOrder(
      desiredOrdering = List("a", "b", "c", "d", "e", "f"),
      unorderedCols = List("i", "f", "c", "g")
    )
    orderedColumns shouldEqual List("c","f","i","g")
  }

  "setColumnOrderAndNullability" should "order input columns and change nullability based on suggested ordering" in {
    val unorderedColumns = Seq(StructField(name="date1", dataType = DateType, nullable = true),
      StructField(name="id", dataType = StringType, nullable = true),
      StructField(name="date2", dataType = DateType, nullable = true),
      StructField(name="double", dataType = DoubleType, nullable = true),
      StructField(name="nestedStruct",dataType = StructType(List(StructField(name="nestedStringField",dataType=StringType,nullable=true))),nullable=false))

    val desiredOrderingAndNullability = Seq(StructField(name="id", dataType = StringType, nullable = false),
      StructField(name="date1", dataType = DateType, nullable = true),
      StructField(name="date2", dataType = DateType, nullable = false),
      StructField(name="date3", dataType = DateType, nullable = false),
      StructField(name="double", dataType = DoubleType, nullable = false),
      StructField(name="date4", dataType = DateType, nullable = false),
      StructField(name="nestedStruct",dataType = StructType(List(StructField(name="nestedStringField",dataType=StringType,nullable=true))),nullable=true))

    val expectedOutput = StructType(Seq(
      StructField(name="id", dataType = StringType, nullable = false),
      StructField(name="date1", dataType = DateType, nullable = true),
      StructField(name="date2", dataType = DateType, nullable = false),
      StructField(name="double", dataType = DoubleType, nullable = false),
      StructField(name="nestedStruct",dataType = StructType(List(StructField(name="nestedStringField",dataType=StringType,nullable=true))))
    ))

    val actual = custDateFacts.setColumnOrderAndNullability(desiredOrderingAndNullability,unorderedColumns)

    expectedOutput shouldEqual actual
  }

  "calculateSMA" should "calculate SMA when only one previous record available" in {
    val smaValues = custDateFacts.calculateSMA(2, testSingleSmaFactsInput)
    smaValues shouldEqual testSingleSmaFactsOutput.drop(1) //The first row in this instance does not have enough preceding values to calculate sma, so it does not get returned.
  }

  "calculateSMA" should "calculate SMA when multiple records are available" in {
    val smaValues = custDateFacts.calculateSMA(2, testMultipleSmaFactsInput)
    smaValues shouldEqual testMultipleSmaFactsOutput.drop(1)
  }

  "calculate30DayEMARecursive" should "calculate EMA when only one previous record available" in {
    val calculateSingleDayEMR = custDateFacts.calculateEMARecursive(testSingleEmaFactsInput, 0.2)
    calculateSingleDayEMR shouldEqual testSingleEmaFactsOutput
  }

  "calculate30DayEMARecursive" should "calculate EMA when multiple records are available" in {
    val calculateMultipleDayEMR = custDateFacts.calculateEMARecursive(testMultipleEmaFactsInput, 0.2)
    calculateMultipleDayEMR shouldEqual testMultipleEmaFactsOutput
  }



}
