package com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate

import com.quantexa.example.scoring.batch.scores.fiu.facts.CustomerDateStatsConfiguration
import org.apache.spark.sql.functions._

import scala.collection.immutable.ListMap

/**
  * This standard deviation follows the method as described in
  * https://math.stackexchange.com/questions/1547141/aggregating-standard-deviation-to-a-summary-point <br>
  * This allows us to pre-aggregate some statistics efficiently in spark, before calculating the standard deviation.
  * This principle would let us have rolling standard deviations for transaction value (at end of day) without encountering
  * Out Of Memory issues in Spark.
  */
object TransactionValueStdDevStats{
  import CommonWindows._
  val configuration =
    CustomerDateStatsConfiguration(
      customerDateLevelStats = ListMap(
        "totalSquareTransactionValueToday" -> sum(pow("analysisAmount", 2))
    ),
      rollingStats = ListMap(
        "totalSquareTransactionValueToDate" -> sum("totalSquareTransactionValueToday").over(customerDateWindow),
        "transactionValueVarianceToDate" ->
          (col("totalSquareTransactionValueToDate")/col("numberOfTransactionsToDate") -
          pow(col("totalTransactionValueToDate")/col("numberOfTransactionsToDate"), 2)),
        "transactionValueStdDevToDate" -> pow("transactionValueVarianceToDate", 0.5)
      )
    )


}



