package com.quantexa.example.scoring.batch.scores.fiu.facts.aggregatedtransaction

import com.quantexa.example.scoring.batch.scores.fiu.facts.AggregatedTransactionConfiguration
import org.apache.spark.sql.functions.{collect_list, collect_set, first, when}

import scala.collection.immutable.ListMap

object AggregatedTransactionMiscAggregations {
  val configuration =
    AggregatedTransactionConfiguration(
      stats = ListMap(
        "currencies" -> collect_set("origCurrency"),
        //FIXME: This list can grow arbitrarirly large and only contains booleans. Should be Map[Boolean,Int].
        "paymentToCustomersOwnAccountAtBank" -> collect_list("paymentToCustomersOwnAccountAtBank"),
        "isCrossBorder" -> when(first("counterpartyAccount.accountCountry") === first("customerAccount.accountCountry"), false).otherwise(true)
      )
    )
}
