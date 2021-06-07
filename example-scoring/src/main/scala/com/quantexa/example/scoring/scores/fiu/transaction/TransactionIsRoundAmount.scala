package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.model.fiu.FactsModel.TransactionFacts
import com.quantexa.example.scoring.model.fiu.RollupModel.TransactionScoreOutput
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput

object TransactionIsRoundAmount extends AugmentedDocumentScore[TransactionFacts, TransactionScoreOutput] {

  val id = "T100_TxnIsRoundAmount"

  val name = "Transaction is round amount"

  def score(transaction: TransactionFacts)(implicit scoreInput: ScoreInput): Option[TransactionScoreOutput] = {

    def createDescription(transactionAmount: Double, txnCurrency:String): String = {
      s"Transaction is round amount ($txnCurrency ${transaction.analysisAmount})."
    }

    val amountOrigCurrency = transaction.amountOrigCurrency

    //This logic is not sufficient for a real project, however set to 100 due to the data available to project example
    if (amountOrigCurrency % 100 == 0) {
      Some(TransactionScoreOutput(keys = transaction.transactionKeys,
        severity = Some(100),
        band = None,
        description = Some(createDescription(amountOrigCurrency, transaction.origCurrency))))
    } else None
  }
}

