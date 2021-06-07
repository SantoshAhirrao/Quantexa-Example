package com.quantexa.example.scoring.batch.scores.fiu.facts.aggregatedTransaction

import java.sql.Date

import com.quantexa.example.scoring.batch.scores.fiu.facts.CalculateAggregatedTransactionFacts.filterByNumberMonths
import com.quantexa.example.scoring.batch.utils.fiu.SparkTestScoringFrameworkTestSuite
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{col, lit, max}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class FilterByNumberMonthsTests extends FilterByNumberMonthsTestScope{

  "filterByNumberMonths function" should "filter the amount within the past twelve months from the latest postingDate of the full input dataset " in {
    val expectedOutput = Array(1, 19, null, null, 70)

    val output: Array[Any] = addedMaxPostingDate.withColumn("filteredAmount",
      filterByNumberMonths(col("postingDate"), col("maxDate"), 12, col("amount"))
    ).select("filteredAmount").collect().flatMap(_.toSeq)

    output.sameElements(expectedOutput)
  }

  it should "filter the postingDate within the past twelve months from the latest postingDate of the full input dataset " in {
    val expectedOutput = Array("2018-01-07", "2018-01-05", null, null, "2018-01-03")

    val output: Array[Any] = addedMaxPostingDate.withColumn("filteredPostingDate",
      filterByNumberMonths(col("postingDate"), col("maxDate"), 12)
    ).select("filteredPostingDate").collect().flatMap(_.toSeq)

    output.sameElements(expectedOutput)
  }

  it should "filter the amount within the past one months from the latest postingDate of the full input dataset " in {
    val expectedOutput = Array(1, null, null, null, null)

    val output: Array[Any] = addedMaxPostingDate.withColumn("filteredAmount",
      filterByNumberMonths(col("postingDate"), col("maxDate"), 1, col("amount"))
    ).select("filteredAmount").collect().flatMap(_.toSeq)

    output.sameElements(expectedOutput)
  }

  it should "filter the postingDate within the past two months from the latest postingDate of the full input dataset " in {
    val expectedOutput = Array("2018-01-07", "2018-01-05", null, null, "2017-11-07")

    val output: Array[Any] = addedMaxPostingDate.withColumn("filteredPostingDate",
      filterByNumberMonths(col("postingDate"), col("maxDate"), 2)
    ).select("filteredPostingDate").collect().flatMap(_.toSeq)

    output.sameElements(expectedOutput)
  }

}

trait FilterByNumberMonthsTestScope extends SparkTestScoringFrameworkTestSuite{
  import spark.implicits._

  val inputTestData: Seq[(Int, Date)] = Seq(
    (1, "2018-01-07"),
    (19, "2017-12-05"),
    (7, "2017-01-05"),
    (5, "2016-01-03"),
    (70, "2017-11-07")
  ).map{
    case (amount, postingDate) => (amount,java.sql.Date.valueOf(postingDate))
  }

  val input: DataFrame = inputTestData.toDF("amount", "postingDate")
  val maxPostingDate: Date = input.select(max(col("postingDate"))).as[java.sql.Date].collect.apply(0)
  val addedMaxPostingDate: DataFrame = input.withColumn("maxDate", lit(maxPostingDate))
}
