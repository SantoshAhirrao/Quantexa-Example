package com.quantexa.example.scoring.batch.model.fiu

import com.quantexa.example.scoring.batch.scores.fiu.scorecards.customer.{CustomerScorecard, CustomerScorecardPostProcessing}
import com.quantexa.example.scoring.model.fiu.FiuDataDependencyProviderDefinitions
import com.quantexa.example.scoring.model.fiu.ScoringModel
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.dependency.{DependencyMissingBehaviour, NoOpIfMissing, SelectionUtils}
import com.quantexa.scoring.framework.model.DataDependencyModel.DataDependencyProviderDefinitions
import com.quantexa.scoring.framework.model.ScoreDefinition.Model.ScoreTypes
import com.quantexa.scoring.framework.model.scoringmodel.ScoringModelDefinition
import com.quantexa.scoring.framework.parameters.{CSVScoreCardGroupParameterSource, CSVScoreCardParameterSource, ScoreParameterProvider}

case class CustomerScorecardModelDefinition(config: ProjectExampleConfig) extends ScoringModelDefinition {
  import com.quantexa.scoring.framework.model.ScoreDefinition.implicits._
  import com.quantexa.scoring.framework.spark.model.SparkModel.implicits._

  val name = "Customer Scorecard Model Definition"
  val requiresSources = false

  val documentTypes = ScoringModel.coreDocumentTypes
  val entityTypes = ScoringModel.coreEntityTypes

  val scores: ScoreTypes = Seq(CustomerScorecard(config))

  val postProcessingScores: ScoreTypes = Seq(CustomerScorecardPostProcessing(config))

  override def dependencyMissingBehaviour: DependencyMissingBehaviour = NoOpIfMissing

  override def dataDependencyProviderDefinitions: Option[DataDependencyProviderDefinitions] = Some(FiuDataDependencyProviderDefinitions(config))

  def phaseRestrictions = Set.empty[SelectionUtils.PhaseRestriction]

  val scorecardParameterPath = "/ScorecardCustomerParameters.csv"
  val scorecardGroupParameters = "/ScorecardCustomerGroups.csv"

  val scoreParameters = new CSVScoreCardParameterSource(scorecardParameterPath, classpath=true)
  val groupParameters = new CSVScoreCardGroupParameterSource(scorecardGroupParameters, classpath=true)
  val parameterProvider = ScoreParameterProvider.fromSources(Seq(scoreParameters, groupParameters))
}
