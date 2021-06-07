package com.quantexa.example.scoring.batch.scores.fiu.integration

import java.nio.file.Paths

import cats.data.NonEmptyList
import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.analytics.spark.scoring.ScoringUtils.WithOutputPath
import com.quantexa.analytics.spark.scoring.scoretypes.MergeDocumentScoresAndCalculateStatistics
import com.quantexa.example.scoring.model.fiu.FactsModel.AggregatedTransactionFacts
import com.quantexa.example.scoring.model.fiu.ScorecardModel.{AggregatedTransactionScores, MergeAggregatedTransactionScores}
import com.quantexa.example.scoring.model.fiu.ScoringModel.{AggregatedTransactionScoreOutput, AggregatedTransactionScoreOutputKeys, AggregatedTransactionScoreOutputWithUnderlying}
import com.quantexa.example.scoring.scores.fiu.aggregatedtransaction.FirstPartyAggregatedTransaction
import com.quantexa.scoring.framework.model.DataDependencyModel.DataDependencyProviderDefinitions
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.spark.model.scores.CustomScore
import org.apache.spark.sql.SparkSession

case class MergeAggregatedTransactionsScores(ddpForStatisticsCalculation: Option[DataDependencyProviderDefinitions] = None)
  extends MergeDocumentScoresAndCalculateStatistics[AggregatedTransactionFacts, AggregatedTransactionScoreOutput, AggregatedTransactionScoreOutputKeys](ddpForStatisticsCalculation) {
  def id: String = "MergeAggregatedTransactionsScores"

  //Override as will run in separate phase
  override def dependencies = Set()

  def scores: NonEmptyList[AugmentedDocumentScore[AggregatedTransactionFacts, AggregatedTransactionScoreOutput]] = NonEmptyList.of(
    FirstPartyAggregatedTransaction
  )
}



object StandardiseAggregatedTransactionsLevelScores extends CustomScore with WithOutputPath {
  private val inputFile = MergeAggregatedTransactionsScores().outputFilePath

  override def score(spark: SparkSession)(implicit scoreInput: ScoreModel.ScoreInput): Any = {
    import spark.implicits._

    val mergeAggregateTransactionScores = spark.read.parquet(inputFile).as[MergeAggregatedTransactionScores]

    val mergeAggregateToAggregatedTransactionScores = mergeAggregateTransactionScores
      .map(x => AggregatedTransactionScores(x.subject, x.keys, x.customScoreOutputMap.mapValues {
        aggregatedTransactionScoreOutput =>
          AggregatedTransactionScoreOutputWithUnderlying(
            keys = aggregatedTransactionScoreOutput.keys,
            severity = aggregatedTransactionScoreOutput.severity,
            band = aggregatedTransactionScoreOutput.band,
            description = aggregatedTransactionScoreOutput.description,
            underlyingScores = Seq.empty
          )
      }
      ))

    mergeAggregateToAggregatedTransactionScores.write.mode("overwrite").parquet(outputFilePath)
  }

  override def outputFilePath: String = Paths.get(outputDirectory, outputFileName).toString

  override def outputFileName: String = MergeAggregatedTransactionsScores().outputFileName.replaceAll("Merged$", "Merged_ForScorecard")

  override def outputDirectory: String = MergeAggregatedTransactionsScores().outputDirectory

  override def id: String = "AddUnderlyingScoresToAggregatedTransactionsLevelScores"

  override def dependencies: Set[ScoreModel.Score] = Set(MergeAggregatedTransactionsScores())
}