package com.quantexa.example.scoring.batch.scores.fiu.integration

import cats.data.NonEmptyList
import com.quantexa.analytics.scala.scoring.model.ScoringModel.ScoreId
import com.quantexa.analytics.scala.scoring.scoretypes._
import com.quantexa.analytics.spark.scoring.scoretypes.{DocumentScoreGenericRollup, _}
import com.quantexa.example.scoring.model.fiu.FactsModel.{CustomerDateFacts, TransactionFacts}
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerDateScoreOutput, CustomerRollup, CustomerScoreOutputWithUnderlying, TransactionScoreOutput}
import com.quantexa.example.scoring.model.fiu.ScoringModel._
import com.quantexa.example.scoring.scores.fiu.rollup._
import com.quantexa.example.scoring.scores.fiu.transaction._
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.DataDependencyModel.DataDependencyProviderDefinitions

//Transaction Score Flow
case class MergeTransactionScores(ddpForStatisticsCalculation: Option[DataDependencyProviderDefinitions] = None)
  extends MergeDocumentScoresAndCalculateStatistics[TransactionFacts, TransactionScoreOutput, TransactionKeys](ddpForStatisticsCalculation) {

  def id: String = "MergeTransactionCustomerDateScores"

  //Override as will run in separate phase
  override def dependencies = Set()

  def scores: NonEmptyList[AugmentedDocumentScore[TransactionFacts, TransactionScoreOutput]] = NonEmptyList.of(
    UnusualCounterpartyCountryForCustomer,
    UnusualBeneficiaryCountryForCustomer,
    UnusualOriginatingCountryForCustomer,
    TransactionIsRoundAmount,
    TransactionIsFirstPartySettlement,
    TxnFromHighToLowRiskCountry
  )
}

object RollupTxnScoreToCustomerLevel
  extends DocumentScoreGenericRollup[TransactionFacts, TransactionScoreOutput, TransactionKeys, CustomerKey] {
  def id: String = "RollupForTransactionsScores"

  override def dependencies = Set()

  def keyToGroupBy = (x: TransactionKeys) => CustomerKey(x.customerId)

  def mergedDocumentScoresInput = MergeTransactionScores()

  override def filterOutputScore = (x:ScoreId,_:TransactionScoreOutput) => !Set()(x)

}

/**
  * @return This will write a Dataframe with schema
  *         <br/>subject:String, keys:CustomerKey, customScoreOutputMap:Map[ScoreId,CustomerScoreOutputWithUnderlying
  *         <br/>to disk when called by the Scoring Framework
  */
case class MergeCustomerLevelTxnScores(config: ProjectExampleConfig) extends
  MergeDocumentScoresAndCalculateStatistics[CustomerRollup[_], CustomerScoreOutputWithUnderlying, CustomerKey] {
  def id: String = "MergeCustomerLevelTxnScores"

  override def dependencies = Set()

  /**
    * Add AugmentedDocumentScores which use the customer rollup of transaction scores here
    */
  def scores: NonEmptyList[AugmentedDocumentScore[CustomerRollup[_], CustomerScoreOutputWithUnderlying]] = NonEmptyList.of(
    CustomerRollupHighNumberOfRoundAmountBatch(java.sql.Date.valueOf(config.runDate)),
    CustomerRollupHighToLowRiskCountryBatch(java.sql.Date.valueOf(config.runDate)),
    CustomerRollupUnusuallyHighDayVolumeBatch(java.sql.Date.valueOf(config.runDate)),
    CustomerRollupUnusualCounterpartyCountryForCustomerBatch(java.sql.Date.valueOf(config.runDate)), //This looks like it's a rewritten version of CustomerRollupUnusualCounterpartyForCustomerBatch
    CustomerRollupUnusualBeneficiaryCountryForCustomerBatch(java.sql.Date.valueOf(config.runDate)),
    CustomerRollupUnusualOriginatingCountryForCustomerBatch(java.sql.Date.valueOf(config.runDate))
  )

}

//Customer Date Score Flow
case class MergeTxnCustomerDateScores(ddpForStatisticsCalculation: Option[DataDependencyProviderDefinitions] = None)
  extends MergeDocumentScoresAndCalculateStatistics[CustomerDateFacts, CustomerDateScoreOutput, CustomerDateKey](ddpForStatisticsCalculation) {
  def id: String = "MergeTransactionInputCustomerDateScores"

  //Override as will run in separate phase
  override def dependencies = Set()

  def scores: NonEmptyList[AugmentedDocumentScore[CustomerDateFacts, CustomerDateScoreOutput]] = NonEmptyList.of(
    UnusuallyHigh1DayValueForCustomer,
    UnusuallyHigh7DaysValueForCustomer,
    UnusuallyHigh30DaysValueForCustomer,
    UnusuallyHigh1DayVolume,
    UnusuallyHigh7DaysVolume,
    UnusuallyHigh30DaysVolume
  )
}


object RollupTxnCustomerDateScoresToCustomerLevel extends DocumentScoreGenericRollup[CustomerDateFacts, CustomerDateScoreOutput, CustomerDateKey, CustomerKey] {
  def id: String = "RollupForTransactionInputCustomerDateScores"

  override def dependencies = Set()

  def keyToGroupBy = (x: CustomerDateKey) => CustomerKey(x.customerId)

  def mergedDocumentScoresInput = MergeTxnCustomerDateScores()

}
