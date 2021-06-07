package com.quantexa.example.etl.projects.fiu.transaction

import com.quantexa.example.model.fiu.transaction.TransactionModel.{AggregatedTransactionParty, Transaction}
import com.quantexa.model.core.datasets.ParsedDatasets.LensParsedBusiness
import org.scalatest.{FlatSpec, Matchers}
import com.quantexa.example.etl.projects.fiu.transaction.TransactionTestData._

class CreateAggregationClassTest extends FlatSpec with Matchers {

  "parseAggregatedParty" should "cleanse the party fields" in {
    val input = AggregatedTransactionParty("1", Some("Tom Geary PLC"), None, None)
    val lensParsedBiz = LensParsedBusiness[AggregatedTransactionParty](
      Some("Tom Geary PLC"), Some("TOM GEARY PLC"), Some(Seq("TOM GEARY")))
    val expectedParty = AggregatedTransactionParty("1", Some("Tom Geary PLC"), None, Some(lensParsedBiz))

    val actualParty = CreateAggregationClass.parseAggregatedParty(input)

    actualParty should be(expectedParty)
  }


  "computeStatistics" should "not compute stats for an empty transaction list" in {
    val testEmptyList = CreateAggregationClass.computeStatistics(Seq.empty)
    assert(testEmptyList.txnSum === None)
    assert(testEmptyList.txnCount === None)
    assert(testEmptyList.txnMin === None)
    assert(testEmptyList.txnMax === None)

  }

  "computeStatistics" should "compute stats for a non-empty transaction list" in {
    val t1: Transaction = testTransaction.copy(txnAmount = 2000)
    val t2: Transaction = testTransaction.copy(txnAmount = 4000)
    val t3: Transaction = testTransaction.copy(txnAmount = 8000)

    val testTxnList = CreateAggregationClass.computeStatistics(Seq(t1, t2, t3): Seq[Transaction])

    assert(testTxnList.txnSum === Some(14000))
    assert(testTxnList.txnCount === Some(3))
    assert(testTxnList.txnMin === Some(2000.0))
    assert(testTxnList.txnMax === Some(8000.0))
  }

  }
