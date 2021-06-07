package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.model.fiu.FactsModel.TransactionFacts
import com.quantexa.example.scoring.model.fiu.RollupModel.TransactionScoreOutput
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.ScoreModel._

object RoundAmountTransactionOfHighRiskCustomer extends AugmentedDocumentScore[TransactionFacts, TransactionScoreOutput] {

  val id = "RoundAmountTransactionOfHighRiskCustomer"

  val name = "Round amount transaction of high risk customer"

  override def dependencies: Set[ScoreModel.Score] = Set(TransactionIsRoundAmount, TransactionOfHighRiskCustomer)

  def score(transaction: TransactionFacts)(implicit scoreInput: ScoreInput): Option[TransactionScoreOutput] = {

    def createDescription(transactionAmount: Double, customerId: Long, customerName: Option[String]): String = {
      if (customerName.isEmpty) s"Transaction is round amount ($transactionAmount) and is of high risk customer ($customerId)"
      else s"Transaction is round amount ($transactionAmount) and is of high risk customer ${customerName.get} ($customerId)"
    }

    val transactionIsRoundAmount: Option[BasicScoreOutput] = scoreInput.previousScoreOutput.get(TransactionIsRoundAmount)
    val transactionOfHighRiskCustomer: Option[BasicScoreOutput] = scoreInput.previousScoreOutput.get(TransactionOfHighRiskCustomer)

    if (transactionIsRoundAmount.isDefined && transactionOfHighRiskCustomer.isDefined) {
      Some(TransactionScoreOutput(keys = transaction.transactionKeys,
        severity = Some(100),
        band = None,
        description = Some(createDescription(transaction.analysisAmount, transaction.customer.customerIdNumber, Some(transaction.customerName)))))
    } else None

  }
}
