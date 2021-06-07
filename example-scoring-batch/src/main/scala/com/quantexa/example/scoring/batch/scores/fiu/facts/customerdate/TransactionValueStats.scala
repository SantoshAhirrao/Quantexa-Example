package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate

import com.quantexa.example.scoring.batch.scores.fiu.facts.CustomerDateStatsConfiguration
import org.apache.spark.sql.functions._

import scala.collection.immutable.ListMap

object TransactionValueStats{
  import CommonWindows._
  val configuration =
    CustomerDateStatsConfiguration(
      customerDateLevelStats = ListMap(
        "totalValueTransactionsToday" -> sum("analysisAmount")
    ),
      rollingStats = ListMap(
        "totalTransactionValueToDate" -> sum("totalValueTransactionsToday").over(customerDateWindow),
        "totalPreviousTransactionValueToDate" -> (col("totalTransactionValueToDate") - col("totalValueTransactionsToday")),
        "totalValueTransactionsOverLast7Days" ->  sum("totalValueTransactionsToday").over(last7Days),
        "totalValueTransactionsOverLast30Days" ->  sum("totalValueTransactionsToday").over(last30Days),
        "previousAverageTransactionAmountToDate" -> mean("totalValueTransactionsToday").over(last365DaysShift1Day),
        "previousStdDevTransactionAmountToDate" -> stddev("totalValueTransactionsToday").over(last365DaysShift1Day),
        "previousAverageTransactionAmountLast7Days" -> mean("totalValueTransactionsOverLast7Days").over(last365DaysShift7Days),
        "previousStdDevTransactionAmountLast7Days" -> stddev("totalValueTransactionsOverLast7Days").over(last365DaysShift7Days),
        "previousAverageTransactionAmountLast30Days" -> mean("totalValueTransactionsOverLast30Days").over(last365DaysShift30Days),
        "previousStdDevTransactionAmountLast30Days" -> stddev("totalValueTransactionsOverLast30Days").over(last365DaysShift30Days)
      )
    )
}



