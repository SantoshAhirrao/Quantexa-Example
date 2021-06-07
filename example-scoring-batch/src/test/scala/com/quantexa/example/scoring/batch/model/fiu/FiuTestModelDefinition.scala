package com.quantexa.example.scoring.batch.model.fiu

import com.quantexa.example.model.fiu.scoring.EdgeAttributes.IndividualEdge
import com.quantexa.example.model.fiu.scoring.EntityAttributes.Individual
import com.quantexa.example.scoring.model.fiu.ScoringModel.{coreDocumentTypes, coreEntityTypes}
import com.quantexa.example.scoring.scores.fiu.entity.{HighRiskCustomerIndividual, IndividualWithIDManipulation, IndividualWithMultipleResidenceCountries}
import com.quantexa.example.scoring.utils.TypedConfigReader.{ElasticSettings, ProjectExampleConfig, ScoreParameterFile, ScoringLookup}
import com.quantexa.scoring.framework.dependency.SelectionUtils
import com.quantexa.scoring.framework.model.ScoreDefinition.Model.{EdgeAttributes, EntityAttributes, ScoreTypes}
import com.quantexa.scoring.framework.model.scoringmodel.ScoringModelDefinition
import com.quantexa.scoring.framework.scala.dependency.ScalaSelectionUtils

object FiuTestModelDefinition extends ScoringModelDefinition {
  import com.quantexa.scoring.framework.model.ScoreDefinition.implicits._

  val name            = "fiu-smoke"
  val requiresSources = true

  val scores: ScoreTypes = Seq(IndividualWithMultipleResidenceCountries,IndividualWithIDManipulation,HighRiskCustomerIndividual)

  val documentTypes = coreDocumentTypes
  val entityTypes = coreEntityTypes

  def phaseRestrictions = SelectionUtils.commonPhaseRestrictions ++ ScalaSelectionUtils.defaultScorePhases
  import scala.reflect.runtime.universe._

  override def linkAttributes: (TypeTag[_ <: EntityAttributes]) => TypeTag[_ <: EdgeAttributes] = (tt:TypeTag[_ <: EntityAttributes]) =>
    tt.tpe match {
      case t if t <:< typeOf[Individual] => typeTag[IndividualEdge]
    }
}
