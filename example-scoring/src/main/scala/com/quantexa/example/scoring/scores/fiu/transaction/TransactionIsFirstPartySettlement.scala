package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.model.fiu.FactsModel.TransactionFacts
import com.quantexa.example.scoring.model.fiu.RollupModel.TransactionScoreOutput
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput

object TransactionIsFirstPartySettlement extends AugmentedDocumentScore[TransactionFacts, TransactionScoreOutput] {

  val id = "T106_TxnIsFirstPartySettlement"

  val name = "Transaction is first party settlement"

  def score(transaction: TransactionFacts)(implicit scoreInput: ScoreInput): Option[TransactionScoreOutput] = {

    def createDescription(transactionAmount: Double, txnCurrency: String): String = {
      s"Transaction is first party settlement ($txnCurrency ${transaction.analysisAmount})."
    }

    val isPaymentToCustomersOwnAccountAtBank = transaction.paymentToCustomersOwnAccountAtBank

    //This logic is not sufficient for a real project, however set to 100 due to the data available to project example
    isPaymentToCustomersOwnAccountAtBank match {
      case Some(true) => Some(
        TransactionScoreOutput(keys = transaction.transactionKeys,
          severity = Some(100),
          band = None,
          description = Some(createDescription(transaction.amountOrigCurrency, transaction.origCurrency))))
      case _ => None
    }

  }
}

