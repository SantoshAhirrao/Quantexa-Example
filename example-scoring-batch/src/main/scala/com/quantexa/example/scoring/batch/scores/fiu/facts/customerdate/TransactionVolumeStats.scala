package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate

import com.quantexa.example.scoring.batch.scores.fiu.facts.CustomerDateStatsConfiguration
import org.apache.spark.sql.functions._

import scala.collection.immutable.ListMap

object TransactionVolumeStats {
  import CommonWindows._
  val configuration =
    CustomerDateStatsConfiguration(
      customerDateLevelStats = ListMap(
        "numberOfTransactionsToday" -> count("*")
        ),
      rollingStats = ListMap(
        "numberOfTransactionsToDate" -> sum("numberOfTransactionsToday").over(customerDateWindow),
        "numberOfPreviousTransactionsToDate" -> (col("numberOfTransactionsToDate") - col("numberOfTransactionsToday")),
        "numberOfTransactionsOverLast7Days" -> sum("numberOfTransactionsToday").over(last7Days),
        "numberOfTransactionsOverLast30Days" -> sum("numberOfTransactionsToday").over(last30Days),
        "previousAverageTransactionVolumeToDate" -> mean("numberOfTransactionsToday").over(last365DaysShift1Day),
        "previousStdDevTransactionVolumeToDate" -> stddev("numberOfTransactionsToday").over(last365DaysShift1Day),
        "previousAverageTransactionVolumeLast7Days" -> mean("numberOfTransactionsOverLast7Days").over(last365DaysShift7Days),
        "previousStdDevTransactionVolumeLast7Days" -> stddev("numberOfTransactionsOverLast7Days").over(last365DaysShift7Days),
        "previousAverageTransactionVolumeLast30Days" -> mean("numberOfTransactionsOverLast30Days").over(last365DaysShift30Days),
        "previousStdDevTransactionVolumeLast30Days" -> stddev("numberOfTransactionsOverLast30Days").over(last365DaysShift30Days))
    )



}
