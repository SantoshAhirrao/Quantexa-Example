package com.quantexa.example.scoring.batch.scores.fiu.facts.customermonth

import com.quantexa.example.scoring.batch.scores.fiu.facts.CustomerDateStatsConfiguration

import scala.collection.immutable.ListMap
import org.apache.spark.sql.functions._
object TransactionValueStats {
  import com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate.CommonWindows._
  val configuration =
    CustomerDateStatsConfiguration(
      customerDateLevelStats = ListMap(
        "totalValueTransactionsThisCalendarMonth" -> sum("analysisAmount")
      ),
      rollingStats = ListMap(
        "totalTransactionValueToDate" -> sum("totalValueTransactionsThisCalendarMonth").over(customerMonthWindow),
        "averageMonthlyTxnValueOver6MonthsExclude1RecentMonth" -> mean("totalValueTransactionsThisCalendarMonth").over(last6MonthsShift1Month),
        "minMonthlyTxnValueOver6MonthsExclude1RecentMonth" -> min("totalValueTransactionsThisCalendarMonth").over(last6MonthsShift1Month),
        "maxMonthlyTxnValueOver6MonthsExclude1RecentMonth" -> max("totalValueTransactionsThisCalendarMonth").over(last6MonthsShift1Month)
      )
    )
}
