package com.quantexa.example.scoring.batch.utils

import com.quantexa.example.scoring.batch.model.fiu.FiuBatchModelDefinition
import com.quantexa.example.scoring.model.fiu.FiuModelDefinition
import com.quantexa.scoring.framework.model.ScoreDefinition.Model.ScoreTypes
import com.quantexa.scoring.framework.model.scoringmodel.ScoringModelDefinition

package object RunScoresInSparkSubmitDefinition {
  sealed abstract class BatchScoreEnum[T<:ScoringModelDefinition](val name: String, //Argument provided by user
                                       val runOrder: Int,
                                       val scoreSetToRun:T => ScoreTypes =  (model:FiuBatchModelDefinition) => Seq.empty)

  case object FactTablesScores extends BatchScoreEnum("facttables",
    1, (model:FiuBatchModelDefinition) => model.factTableScores) with StandardInvocation

  case object TransactionScores extends BatchScoreEnum("transactionscores",
    2, (model:FiuModelDefinition) => model.transactionScores) with StandardInvocation

  case object MergeTransactionScores extends BatchScoreEnum("mergetransactionscores",
    3, (model:FiuBatchModelDefinition) => model.mergeDocumentScores) with StandardInvocation

  case object RollupFromMergedDocumentScores extends BatchScoreEnum("rolluptransactionscores",
    4, (model:FiuBatchModelDefinition) => model.rollupFromMergedDocumentScores) with StandardInvocation

  case object CustomerScores extends BatchScoreEnum("customerscores",
    5, (model:FiuModelDefinition) => model.customerScores) with StandardInvocation

  case object MergeCustomerScores extends BatchScoreEnum("mergecustomerscores",
    6, (model:FiuBatchModelDefinition) => model.mergeCustomerScores) with StandardInvocation

  case object DocumentScores extends BatchScoreEnum("documentscores",
    7, (model:FiuModelDefinition) => model.docScores) with StandardInvocation

  case object EntityScores extends BatchScoreEnum("entityscores",
    8, (model:FiuModelDefinition) => model.entityScores) with StandardInvocation

  case object NetworkScores extends BatchScoreEnum("networkscores",
    9, (model:FiuModelDefinition) => model.networkScores) with StandardInvocation

  case object Scorecard extends BatchScoreEnum("scorecard", 
    10) with ScorecardInvocation

  case object PostprocessScorecard extends BatchScoreEnum("postprocessscorecard", 
    11) with ScorecardInvocation

  case object OtherScores extends BatchScoreEnum("otherscores",
    12) with OtherInvocation

  sealed trait InvocationMethod
  sealed trait StandardInvocation extends InvocationMethod
  sealed trait ScorecardInvocation extends InvocationMethod
  sealed trait OtherInvocation extends InvocationMethod
}