package com.quantexa.example.scoring.batch.model.fiu

import com.quantexa.example.scoring.batch.scores.fiu.facts._
import com.quantexa.example.scoring.batch.scores.fiu.integration._
import com.quantexa.example.model.fiu.scoring.EdgeAttributes.{BusinessEdge, IndividualEdge}
import com.quantexa.example.model.fiu.scoring.EntityAttributes.{Business, Individual}
import com.quantexa.example.scoring.model.fiu.ScoringModel
import com.quantexa.example.scoring.scores.fiu.aggregatedtransaction.FirstPartyAggregatedTransaction
import com.quantexa.example.scoring.scores.fiu.rollup._
import com.quantexa.example.scoring.scores.fiu.document.customer._
import com.quantexa.example.scoring.scores.fiu.entity._
import com.quantexa.example.scoring.scores.fiu.scorecards.ExampleScorecard
import com.quantexa.example.scoring.scores.fiu.transaction._
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.dependency.{DependencyMissingBehaviour, WarnIfMissing}
import com.quantexa.scoring.framework.model.DataDependencyModel.DataDependencyProviderDefinitions
import com.quantexa.scoring.framework.model.ScoreDefinition.Model.{EdgeAttributes, EntityAttributes, ScoreTypes}
import com.quantexa.scoring.framework.model.scoringmodel.ScoringModelDefinition
import com.quantexa.scoring.framework.scala.dependency.ScalaSelectionUtils

import scala.reflect.runtime.universe._

case class FiuBatchModelDefinition(config: ProjectExampleConfig) extends ScoringModelDefinition {
  import com.quantexa.scoring.framework.model.ScoreDefinition.implicits._
  import com.quantexa.scoring.framework.spark.model.SparkModel.implicits._

  val name            = "fiu-smoke-batch"
  val requiresSources = true

  val documentTypes = ScoringModel.coreDocumentTypes

  val entityTypes = ScoringModel.coreEntityTypes

  override def dataDependencyProviderDefinitions: Option[DataDependencyProviderDefinitions] =
    Some(FiuBatchDataDependencyProviderDefinitions(config))

  //This has been added to allow us to add the statistics for the customer and aggregated transaction scores.
  //Removes the error: Exception in thread "main" java.lang.Exception: Not all dependency IDs exists in score IDs.
 override def dependencyMissingBehaviour: DependencyMissingBehaviour = WarnIfMissing

  def phaseRestrictions = ScalaSelectionUtils.defaultScorePhases

  //This contains the link attributes case classes, should be populated when attributes are used in scores.
  override val linkAttributes : (TypeTag[_ <: EntityAttributes]) => TypeTag[_ <: EdgeAttributes] = (tt : TypeTag[_ <: EntityAttributes]) => {
    tt.tpe match {
      case t if t <:< typeOf[Business]   => typeTag[BusinessEdge]
      case t if t <:< typeOf[Individual]   => typeTag[IndividualEdge]
    }
  }

  val factTableScores: ScoreTypes = Seq(
    CalculateAggregatedTransactionFacts(config),
    CalculateTransactionFacts(config),
    CalculateCustomerDateFacts(config),
    CalculateCustomerMonthFacts(config)
  )

  val customerDocumentScores: ScoreTypes  = Seq(
    HighRiskCustomerDiscrete,
    HighRiskCustomerContinuous,
    NewCustomer,
    CancelledCustomer,
    CustomerFromHighRiskCountry,
    CustomerAddressInLowValuePostcode
  )

  val transactionDocumentScores: ScoreTypes = Seq(CustomerAccountCountryRisk,
    CounterpartyAccountCountryRisk,
    TxnFromHighToLowRiskCountry,
    TxnFromLowToHighRiskCountry,
    TransactionIsFirstPartySettlement,
    TransactionIsRoundAmount,
    UnusualBeneficiaryCountryForCustomer,
    UnusualOriginatingCountryForCustomer,
    UnusualCounterpartyCountryForCustomer,
    UnusuallyHigh1DayVolume,
    UnusuallyHigh7DaysVolume,
    UnusuallyHigh30DaysVolume,
    UnusuallyHigh1DayValueForCustomer,
    UnusuallyHigh7DaysValueForCustomer,
    UnusuallyHigh30DaysValueForCustomer
  )

  val customerRollupScores: ScoreTypes = Seq(
    CustomerRollupHighNumberOfRoundAmountBatch(config.runDateDt),
    CustomerRollupHighToLowRiskCountryBatch(config.runDateDt),
    CustomerRollupUnusuallyHighDayVolumeBatch(config.runDateDt),
    CustomerRollupUnusualCounterpartyCountryForCustomerBatch(config.runDateDt),
    CustomerRollupUnusualBeneficiaryCountryForCustomerBatch(config.runDateDt),
    CustomerRollupUnusualOriginatingCountryForCustomerBatch(config.runDateDt)
  )

  val aggregatedTransactionDocumentScores: ScoreTypes = Seq(
    FirstPartyAggregatedTransaction
  )

  val mergeDocumentScores: ScoreTypes  = Seq(
    MergeTransactionScores(ddpForStatisticsCalculation = dataDependencyProviderDefinitions),
    MergeTxnCustomerDateScores(ddpForStatisticsCalculation = dataDependencyProviderDefinitions),
    MergeAggregatedTransactionsScores(ddpForStatisticsCalculation = dataDependencyProviderDefinitions),
    StandardiseAggregatedTransactionsLevelScores,
    CreateAggregatedTransactionScorecardInput(config)
  )

  val rollupFromMergedDocumentScores: ScoreTypes = Seq(RollupTxnScoreToCustomerLevel, RollupTxnCustomerDateScoresToCustomerLevel)

  val mergeCustomerScores: ScoreTypes = Seq(
    MergeCustomerScores(ddpForStatisticsCalculation = dataDependencyProviderDefinitions),
    StandardiseCustomerLevelScores,
    MergeCustomerLevelTxnScores(config),
    CreateCustomerScorecardInput(config)
  )

  val entityScores: ScoreTypes = Seq(
    HighRiskCustomerIndividual,
    IndividualWithMultipleResidenceCountries,
    IndividualWithIDManipulation,
    IndividualOnHotlist,
    RecentlyJoinedBusiness,
    BusinessOnHotlist
  )
  val networkScores: ScoreTypes = Seq(
  )

  val scorecardScores: ScoreTypes = Seq(
    ExampleScorecard
  )

  /**
    * The scoring framework requires this to be defined, we don't call this directly
    * and subsets of scores would be run at a given time.
    */
  val scores: ScoreTypes = factTableScores ++
    customerDocumentScores ++
    transactionDocumentScores ++
    aggregatedTransactionDocumentScores ++
    customerRollupScores ++
    mergeDocumentScores ++
    rollupFromMergedDocumentScores ++
    entityScores ++
    networkScores ++
    scorecardScores

}
