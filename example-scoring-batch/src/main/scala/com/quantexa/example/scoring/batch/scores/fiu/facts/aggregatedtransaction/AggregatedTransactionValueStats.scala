package com.quantexa.example.scoring.batch.scores.fiu.facts.aggregatedtransaction

import com.quantexa.example.scoring.batch.scores.fiu.facts.AggregatedTransactionConfiguration
import org.apache.spark.sql.functions._
import com.quantexa.example.scoring.batch.scores.fiu.facts.CalculateAggregatedTransactionFacts._

import scala.collection.immutable.ListMap

object AggregatedTransactionValueStats {
  val configuration = AggregatedTransactionConfiguration(
    stats = ListMap(
      "totalTransactionsAmount" -> sum("analysisAmount"),
      "minTransactionAmount" -> min("analysisAmount"),
      "maxTransactionAmount" -> max("analysisAmount"),
      "totalTransactionsAmountOverLast365Days" -> sum(filterByNumberMonths(col("postingDate"), col("maxDate"), 12, col("analysisAmount"))),
      "minTransactionAmountOverLast365Days" -> min(filterByNumberMonths(col("postingDate"), col("maxDate"), 12, col("analysisAmount"))),
      "maxTransactionAmountOverLast365Days" -> max(filterByNumberMonths(col("postingDate"), col("maxDate"), 12, col("analysisAmount")))
    )
  )
}