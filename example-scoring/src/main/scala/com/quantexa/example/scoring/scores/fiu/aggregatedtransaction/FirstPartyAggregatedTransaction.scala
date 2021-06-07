package com.quantexa.example.scoring.scores.fiu.aggregatedtransaction

import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.example.scoring.model.fiu.FactsModel.AggregatedTransactionFacts
import com.quantexa.example.scoring.model.fiu.ScoringModel.{AggregatedTransactionScoreOutputKeys, AggregatedTransactionScoreOutput}
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput

object FirstPartyAggregatedTransaction extends AugmentedDocumentScore[AggregatedTransactionFacts, AggregatedTransactionScoreOutput] {

  val id = "AT100_FirstPartyAggregatedTransaction"

  val name = "Aggregated Transaction is a first party settlement"

  def score(aggregatedTransaction: AggregatedTransactionFacts)(implicit scoreInput: ScoreInput): Option[AggregatedTransactionScoreOutput] = {

    if(aggregatedTransaction.paymentToCustomersOwnAccountAtBank contains true) {
      Some(
        AggregatedTransactionScoreOutput(keys = AggregatedTransactionScoreOutputKeys(
          aggregatedTransaction.aggregatedTransactionId,
          aggregatedTransaction.customerId,
          aggregatedTransaction.counterpartyAccountId),
          severity = Some(100),
          band = None,
          description = Some(s"Aggregated Transaction ${aggregatedTransaction.aggregatedTransactionId} is a first party settlement")))
    } else None
  }
}
