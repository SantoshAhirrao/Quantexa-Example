package com.quantexa.example.scoring.batch.model.fiu

import com.quantexa.example.scoring.batch.scores.fiu.scorecards.aggregatedTransaction.{AggregatedTransactionScorecard, AggregatedTransactionScorecardToCustomerScore}
import com.quantexa.example.scoring.model.fiu.FiuDataDependencyProviderDefinitions
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.dependency.{DependencyMissingBehaviour, NoOpIfMissing, SelectionUtils}
import com.quantexa.scoring.framework.model.DataDependencyModel.DataDependencyProviderDefinitions
import com.quantexa.scoring.framework.model.ScoreDefinition.Model.{DocumentTypes, EntityTypes, ScoreTypes}
import com.quantexa.scoring.framework.model.scoringmodel.ScoringModelDefinition
import com.quantexa.scoring.framework.parameters.{CSVScoreCardGroupParameterSource, CSVScoreCardParameterSource, ScoreParameterProvider}

case class AggregatedTransactionsScorecardModelDefinition (config: ProjectExampleConfig) extends ScoringModelDefinition {
  import com.quantexa.scoring.framework.model.ScoreDefinition.implicits._
  import com.quantexa.scoring.framework.spark.model.SparkModel.implicits._

  override def name: String = "Aggregated Transactions Scorecard Model Definition"

  override def requiresSources: Boolean = false

  override def documentTypes: DocumentTypes = Seq.empty

  override def entityTypes: EntityTypes = Seq.empty

  override def scores: ScoreTypes = Seq(AggregatedTransactionScorecard(config), AggregatedTransactionScorecardToCustomerScore(config))

  override def phaseRestrictions: Set[SelectionUtils.PhaseRestriction] = Set.empty[SelectionUtils.PhaseRestriction]

  override def dependencyMissingBehaviour: DependencyMissingBehaviour = NoOpIfMissing

  override def dataDependencyProviderDefinitions: Option[DataDependencyProviderDefinitions] = Some(FiuDataDependencyProviderDefinitions(config))

  val scorecardParameterPath = "/ScorecardAggregatedTransactionParameters.csv"
  val scorecardGroupParameters = "/ScorecardAggregatedTransactionGroups.csv"

  val scoreParameters = new CSVScoreCardParameterSource(scorecardParameterPath, classpath=true)
  val groupParameters = new CSVScoreCardGroupParameterSource(scorecardGroupParameters, classpath=true)
  val parameterProvider = ScoreParameterProvider.fromSources(Seq(scoreParameters, groupParameters))
}
