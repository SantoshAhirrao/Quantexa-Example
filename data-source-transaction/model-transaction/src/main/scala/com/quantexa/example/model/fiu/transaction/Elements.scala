package com.quantexa.example.model.fiu.transaction

import com.quantexa.example.model.fiu.transaction.TransactionModel.AggregatedTransactionAccount
import com.quantexa.resolver.ingest.BrokenLenses.BOpticOps.elementLens

object Elements {

  val accountId = elementLens[AggregatedTransactionAccount]("accountId")(account => {
    List(account.accountId)
  })

  val accountNumber = elementLens[AggregatedTransactionAccount]("accountNumber")(account => {
    List(account.accountNumber)
  })

  // TODO add sort code
//  val sortCode = ???
}
