package com.quantexa.example.scoring.batch.scores.fiu.facts.aggregatedtransaction

import com.quantexa.example.scoring.batch.scores.fiu.facts.AggregatedTransactionConfiguration
import org.apache.spark.sql.functions._
import com.quantexa.example.scoring.batch.scores.fiu.facts.CalculateAggregatedTransactionFacts._

import scala.collection.immutable.ListMap

object AggregatedTransactionVolumeStats {
  val configuration =
    AggregatedTransactionConfiguration(
      stats = ListMap(
        "numberOfTransactionsToDate" -> count("*"),
        "numberOfTransactionsOverLast365Days" -> count(filterByNumberMonths(col("postingDate"), col("maxDate"), 12))
      )
    )
}
