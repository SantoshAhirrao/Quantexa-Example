package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, unix_timestamp}

object CommonWindows {
  private val YearInDays = 365
  private def daysToSeconds(numDays: Int): Int = numDays * 24 * 60 * 60
  val customerDateWindow = Window.partitionBy("customerId").orderBy(unix_timestamp(col("analysisDate")))
  val customerMonthWindow = Window.partitionBy("customerId").orderBy(col("monthsSinceEpoch"))

  val last6MonthsShift1Month = Window.partitionBy("customerId").orderBy(col("monthsSinceEpoch")).rangeBetween(-6, -1)

  val last7Days = Window.partitionBy("customerId").orderBy(unix_timestamp(col("analysisDate"))).rangeBetween(daysToSeconds(-6), 0)
  val last30Days = Window.partitionBy("customerId").orderBy(unix_timestamp(col("analysisDate"))).rangeBetween(daysToSeconds(-29), 0)

  val last365DaysShift1Day = Window.partitionBy("customerId").orderBy(unix_timestamp(col("analysisDate"))).rangeBetween(daysToSeconds(-YearInDays), daysToSeconds(-1))
  val last365DaysShift7Days = Window.partitionBy("customerId").orderBy(unix_timestamp(col("analysisDate"))).rangeBetween(daysToSeconds(-YearInDays - 6), daysToSeconds(-7))
  val last365DaysShift30Days = Window.partitionBy("customerId").orderBy(unix_timestamp(col("analysisDate"))).rangeBetween(daysToSeconds(-YearInDays - 29), daysToSeconds(-30))

  val allTimeShift1Day = Window.partitionBy("customerId").orderBy(unix_timestamp(col("analysisDate"))).rangeBetween(Long.MinValue, daysToSeconds(-1))
}
