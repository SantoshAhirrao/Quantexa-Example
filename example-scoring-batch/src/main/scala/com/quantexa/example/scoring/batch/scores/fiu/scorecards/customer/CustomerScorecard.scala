package com.quantexa.example.scoring.batch.scores.fiu.scorecards.customer

import java.nio.file.Paths

import com.quantexa.analytics.spark.scoring.ScoringUtils.WithOutputPath
import com.quantexa.example.scoring.batch.scores.fiu.integration.CreateCustomerScorecardInput
import com.quantexa.example.scoring.scores.fiu.document.customer._
import com.quantexa.example.scoring.scores.fiu.rollup._
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.scores.ScoreCardScore
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, ScoreParameterIdentifier}
import com.quantexa.scoring.framework.scorecarding.ScoreCardConfiguration
import java.sql.Date

case class CustomerScorecard(config: ProjectExampleConfig) extends ScoreCardScore with WithOutputPath {
  def id: String = "CustomerScorecard"

  override def scoreCardConfiguration: ScoreCardConfiguration = ScoreCardWithInputPath(config)

  def outputDirectory: String = (Paths.get(config.hdfsFolderScoring) resolve Paths.get("ScoreCard")).toString
  def outputFileName: String = this.id
  def outputFilePath: String = Paths.get(outputDirectory, outputFileName).toString

  override def dependencies: Set[ScoreModel.Score] = Set(HighRiskCustomerDiscrete,
    NewCustomer,
    CancelledCustomer,
    CustomerFromHighRiskCountry,
    CustomerAddressInLowValuePostcode,
    CustomerRollupUnusuallyHighDayVolumeOnDemand(Date.valueOf(config.runDate)),
    CustomerRollupHighToLowRiskCountryOnDemand(Date.valueOf(config.runDate)),
    CustomerRollupHighNumberOfRoundAmountOnDemand(Date.valueOf(config.runDate)),
    CustomerRollupUnusualBeneficiaryCountryForCustomerOnDemand(Date.valueOf(config.runDate)),
    CustomerRollupUnusualCounterpartyCountryForCustomerOnDemand(Date.valueOf(config.runDate)),
    CustomerRollupUnusualOriginatingCountryForCustomerOnDemand(Date.valueOf(config.runDate))
  )

  def groupParameters = dependencies.map(score => ParameterIdentifier(None, score.id))

  override def parameters: Set[ScoreParameterIdentifier] = groupParameters.map(_.asInstanceOf[ScoreParameterIdentifier])

}

case class ScoreCardWithInputPath(config: ProjectExampleConfig) extends ScoreCardConfiguration {
  override def inputFilePath: String = CreateCustomerScorecardInput(config).outputFilePath
}
