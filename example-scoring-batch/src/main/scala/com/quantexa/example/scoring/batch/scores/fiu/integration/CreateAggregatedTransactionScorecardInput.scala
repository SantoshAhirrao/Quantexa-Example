package com.quantexa.example.scoring.batch.scores.fiu.integration

import java.nio.file.Paths

import com.quantexa.analytics.spark.scoring.ScoringUtils.WithOutputPath
import com.quantexa.example.scoring.model.fiu.ScorecardModel.AggregatedTransactionScores
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.scoring.framework.scorecarding.ScoringModels.StandardBasicScoreOutput
import com.quantexa.scoring.framework.spark.model.scores.CustomScore
import org.apache.spark.sql.{Dataset, SparkSession}


case class CreateAggregatedTransactionScorecardInput(config: ProjectExampleConfig) extends CustomScore with WithOutputPath {

  val standardisedScores: Seq[ScoreModel.Score with WithOutputPath] = Seq(StandardiseAggregatedTransactionsLevelScores)

  def outputFilePathForUnderlying: String = Paths.get(outputDirectory,"AggregatedTransactionsScoreCardInputWithUnderlying.parquet").toString
  private val inputFilePaths = standardisedScores.map(_.outputFilePath)

  override def score(spark: SparkSession)(implicit scoreInput: ScoreModel.ScoreInput): Any = {
    import spark.implicits._
    implicit val sparkSession: SparkSession = spark

    val aggregatedTransactionsScoresWithUnderlying = inputFilePaths.map(spark.read.parquet(_).as[AggregatedTransactionScores]).reduce(_ union _)

    val scorecardInputWithUnderlying = mergeScoreInputsByAggregatedTransactionKey(aggregatedTransactionsScoresWithUnderlying)
    scorecardInputWithUnderlying.write.mode("overwrite").parquet(outputFilePathForUnderlying)

    val scorecardInputStandard: Dataset[StandardBasicScoreOutput] = scoreCardInputToStandardisedBSO(aggregatedTransactionsScoresWithUnderlying)
    scorecardInputStandard.write.mode("overwrite").parquet(outputFilePath)
  }

  override def outputFilePath: String = Paths.get(outputDirectory, outputFileName).toString

  override def outputFileName: String = "CreateAggregatedTransactionScorecardInput.parquet"

  override def outputDirectory: String = config.hdfsFolderScoring.toString

  override def id: String = "MergeAllAggregatedTransactionLevelScores"

  override def dependencies: Set[ScoreModel.Score] = standardisedScores.toSet


  /**
    *
    * @param aggregatedTransactionsScoresWithUnderlying
    * @return A Dataset which contains all of the original fields on the scorecard input
    */
  private def mergeScoreInputsByAggregatedTransactionKey(aggregatedTransactionsScoresWithUnderlying: Dataset[AggregatedTransactionScores])(implicit spark:SparkSession) = {
    import spark.implicits._
    aggregatedTransactionsScoresWithUnderlying.groupByKey(x => (x.subject, x.keys)).
      mapGroups {
        case ((subjectId, keys), scoreCardInputIter) =>
          val scoresMap = scoreCardInputIter.map(_.customScoreOutputMap).reduce(_ ++ _)
          AggregatedTransactionScores(subject = subjectId,
            keys = keys,
            customScoreOutputMap = scoresMap)
      }
  }


  /**
    * @param scorecardInputWithUnderlying
    * @return A Dataset compatible with the ScoreCard score type
    */
  private def scoreCardInputToStandardisedBSO(scorecardInputWithUnderlying: Dataset[AggregatedTransactionScores])(implicit spark:SparkSession) = {
    import spark.implicits._
    scorecardInputWithUnderlying.map {
      scorecardInput =>
        val basicScoreOutputMap = scorecardInput.customScoreOutputMap.map {
          scoreWithUnderlying =>
            val (scoreId, scoreOutput) = scoreWithUnderlying
            scoreId -> BasicScoreOutput(severity = scoreOutput.severity, band = scoreOutput.band, description = scoreOutput.description)
        }
        StandardBasicScoreOutput(id = scorecardInput.subject, basicScoreOutputMap = basicScoreOutputMap)
    }
  }

}
