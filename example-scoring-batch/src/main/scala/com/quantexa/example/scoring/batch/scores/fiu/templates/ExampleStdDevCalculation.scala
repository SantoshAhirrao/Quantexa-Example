package com.quantexa.example.scoring.batch.scores.fiu.templates

import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.functions._

object ExampleStdDevCalculation {

  case class MinimalTransaction(
                                 analysisDate : Int,
                                 analysisAmount : Double,
                                 customerId : Int
                               )


  def calculateStandardDeviationFromPartials(transactions: Dataset[MinimalTransaction]): DataFrame = {
    // Calculate population standard deviation for each customerId in dataset by first pre-aggregating transactions on a daily basis
    // uses sum of squares method with formula: variance = sum(x^2)/n - (sum(x)/n)^2
    // reference: https://math.stackexchange.com/questions/1547141/aggregating-standard-deviation-to-a-summary-point


    import transactions.sparkSession.implicits._

    val simpleAggregateStats =  transactions.groupBy("customerId","analysisDate").agg(
      count("*").as("numberOfTransactions")
      ,sum("analysisAmount").as("totalValueTransactions")
      ,sum(pow("analysisAmount",2)).as("sumOfSquares"))


    val simpleAggStatsByCustomer = simpleAggregateStats.groupBy("customerId")

    val totalNumberTransactions = simpleAggStatsByCustomer.agg(
      sum("numberOfTransactions").as("sum(numberOfTransactions)")
      ,sum("totalValueTransactions").as("sum(totalValueTransactions)")
      ,sum("sumOfSquares").as("sum(sumOfSquares)"))

    val stDev = totalNumberTransactions.
      withColumn("variance", ($"sum(sumOfSquares)" / $"sum(numberOfTransactions)") -
        pow($"sum(totalValueTransactions)" / $"sum(numberOfTransactions)".cast("Double") ,2)).
      withColumn("standardDeviation",sqrt($"variance")).
      select("customerId","standardDeviation")

    stDev

  }

  def calcStDevMethodSpark(transactions: Dataset[MinimalTransaction]): DataFrame = {
    // Calculate population standard deviation for each customerID in dataset using spark internal method
    val transactionsByCustomer = transactions.groupBy("customerId")
    val stDev = transactionsByCustomer.
      agg(stddev_pop("analysisAmount").as("standardDeviation")).
      select("customerId","standardDeviation")

    stDev
  }


}

