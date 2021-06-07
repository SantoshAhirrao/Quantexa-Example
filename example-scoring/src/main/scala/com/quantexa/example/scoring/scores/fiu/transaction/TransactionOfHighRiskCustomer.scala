package com.quantexa.example.scoring.scores.fiu.transaction

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.model.fiu.FactsModel.TransactionFacts
import com.quantexa.example.scoring.model.fiu.RollupModel.TransactionScoreOutput
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.example.scoring.scores.fiu.document.customer.HighRiskCustomerDiscrete

object TransactionOfHighRiskCustomer extends AugmentedDocumentScore[TransactionFacts, TransactionScoreOutput] {

  val id = "TransactionOfHighRiskCustomer"

  val name = "Transaction of high risk customer"

  def createDescription(customerName: String, customerID: String): String = {
    if(customerName == null) s"Transaction of high risk customer ($customerID)."
    else
      s"Transaction of high risk customer $customerName ($customerID)."
  }

  override def score(transaction: TransactionFacts)(implicit scoreInput: ScoreInput): Option[TransactionScoreOutput] = {

    val highRiskCustomerScoreOutput = HighRiskCustomerDiscrete.score(transaction.customer)

    highRiskCustomerScoreOutput.map(scoreOutput =>
      TransactionScoreOutput(
        keys=transaction.transactionKeys,
        band = None,
        description = Some(createDescription(transaction.customerName, transaction.customerId)),
        severity = scoreOutput.severity)
    )
  }
}

