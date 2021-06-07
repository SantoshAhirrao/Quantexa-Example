package com.quantexa.example.scoring.model.fiu

import com.quantexa.example.model.fiu.scoring.EdgeAttributes.{BusinessEdge, IndividualEdge}
import com.quantexa.example.model.fiu.scoring.EntityAttributes.{Business, Individual}
import com.quantexa.example.scoring.model.fiu.ScoringModel.{coreDocumentTypes, coreEntityTypes}
import com.quantexa.example.scoring.scores.fiu.document.customer._
import com.quantexa.example.scoring.scores.fiu.entity._
import com.quantexa.example.scoring.scores.fiu.network.HighNumberOfHotlistDocs
import com.quantexa.example.scoring.scores.fiu.rollup._
import com.quantexa.example.scoring.scores.fiu.scorecards.ExampleScorecard
import com.quantexa.example.scoring.scores.fiu.transaction._
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.dependency.SelectionUtils
import com.quantexa.scoring.framework.model.DataDependencyModel.DataDependencyProviderDefinitions
import com.quantexa.scoring.framework.model.ScoreDefinition.Model.{EdgeAttributes, EntityAttributes, ScoreTypes}
import com.quantexa.scoring.framework.model.scoringmodel.ScoringModelDefinition
import com.quantexa.scoring.framework.scala.dependency.ScalaSelectionUtils

import scala.reflect.runtime.universe._

case class FiuModelDefinition(config:ProjectExampleConfig) extends ScoringModelDefinition {
  import com.quantexa.scoring.framework.model.ScoreDefinition.implicits._

  val name            = "fiu-smoke"
  val requiresSources = true

  /** NOTE: This is hardcoded here because it was hardcoded when the batch scores where run. In practice this value should
    * be set to the current date. it should remain a def so that you always take the current date.
    * */
  private def runDate: java.sql.Date = java.sql.Date.valueOf("2017-08-01")

  val customerRollupScores: ScoreTypes = Seq(
    CustomerRollupUnusuallyHighDayVolumeOnDemand(runDate),
    CustomerRollupHighToLowRiskCountryOnDemand(runDate),
    CustomerRollupHighNumberOfRoundAmountOnDemand(runDate),
    CustomerRollupUnusualBeneficiaryCountryForCustomerOnDemand(runDate),
    CustomerRollupUnusualCounterpartyCountryForCustomerOnDemand(runDate),
    CustomerRollupUnusualOriginatingCountryForCustomerOnDemand(runDate)
  )

  val customerScores: ScoreTypes  = Seq(
		HighRiskCustomerDiscrete,
    HighRiskCustomerContinuous,
		NewCustomer,
    CancelledCustomer)

  val transactionScores: ScoreTypes = Seq(
    CustomerAccountCountryRisk,
    CounterpartyAccountCountryRisk,
    TxnFromHighToLowRiskCountry,
    TxnFromLowToHighRiskCountry
   )

  val docScores: ScoreTypes = customerScores ++ transactionScores

  val entityScores: ScoreTypes = Seq(
    //PhoneHit,
    HighRiskCustomerIndividual,
    IndividualWithMultipleResidenceCountries,
    IndividualWithIDManipulation,
    IndividualOnHotlist,
//    RecentlyJoinedBusiness,
    BusinessOnHotlist
  )
  val networkScores: ScoreTypes = Seq(
//    TotalHighRiskCustomersOnNetwork,
    HighNumberOfHotlistDocs
  )

  val scorecardScores: ScoreTypes = Seq(
    ExampleScorecard
  )

  val scores: ScoreTypes = docScores ++ customerRollupScores ++ entityScores ++ networkScores ++ scorecardScores

  val documentTypes = coreDocumentTypes

  val entityTypes = coreEntityTypes

  override def dataDependencyProviderDefinitions: Option[DataDependencyProviderDefinitions] = Some(FiuDataDependencyProviderDefinitions(config))

  def phaseRestrictions = SelectionUtils.commonPhaseRestrictions ++ ScalaSelectionUtils.defaultScorePhases

  override val linkAttributes : (TypeTag[_ <: EntityAttributes]) => TypeTag[_ <: EdgeAttributes] = (tt : TypeTag[_ <: EntityAttributes]) => {
    tt.tpe match {
      case t if t <:< typeOf[Individual] => typeTag[IndividualEdge]
      case t if t <:< typeOf[Business]   => typeTag[BusinessEdge]
    }
  }
}