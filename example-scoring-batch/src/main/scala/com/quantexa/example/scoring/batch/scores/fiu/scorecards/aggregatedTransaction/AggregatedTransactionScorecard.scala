package com.quantexa.example.scoring.batch.scores.fiu.scorecards.aggregatedTransaction

import java.nio.file.Paths

import com.quantexa.analytics.spark.scoring.ScoringUtils.WithOutputPath
import com.quantexa.example.scoring.batch.scores.fiu.integration.CreateAggregatedTransactionScorecardInput
import com.quantexa.example.scoring.scores.fiu.aggregatedtransaction.FirstPartyAggregatedTransaction
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.scores.ScoreCardScore
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, ScoreParameterIdentifier}
import com.quantexa.scoring.framework.scorecarding.ScoreCardConfiguration

case class AggregatedTransactionScorecard (config: ProjectExampleConfig) extends ScoreCardScore with WithOutputPath {
  override def scoreCardConfiguration: ScoreCardConfiguration = ScoreCardWithInputPath(config)

  override def outputFilePath: String = Paths.get(outputDirectory, outputFileName).toString

  override def outputFileName: String = this.id

  override def outputDirectory: String = (Paths.get(config.hdfsFolderScoring) resolve Paths.get("ScoreCard")).toString

  override def parameters: Set[ScoreParameterIdentifier] = groupParameters.map(_.asInstanceOf[ScoreParameterIdentifier])

  override def id: String = "AggregatedTransactionScorecard"

  override def dependencies: Set[ScoreModel.Score] = Set(FirstPartyAggregatedTransaction)

  def groupParameters: Set[ParameterIdentifier] = dependencies.map(score => ParameterIdentifier(None, score.id))
}

case class ScoreCardWithInputPath(config: ProjectExampleConfig) extends ScoreCardConfiguration {
  override def inputFilePath: String = CreateAggregatedTransactionScorecardInput(config).outputFilePath
}
