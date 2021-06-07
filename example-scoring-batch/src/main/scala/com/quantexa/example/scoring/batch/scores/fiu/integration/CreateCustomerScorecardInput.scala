package com.quantexa.example.scoring.batch.scores.fiu.integration

import java.nio.file.Paths

import com.quantexa.analytics.spark.scoring.ScoringUtils.WithOutputPath
import com.quantexa.example.scoring.batch.scores.fiu.scorecards.aggregatedTransaction.AggregatedTransactionScorecardToCustomerScore
import com.quantexa.example.scoring.model.fiu.ScorecardModel.CustomerScores
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.scoring.framework.scorecarding.ScoringModels.StandardBasicScoreOutput
import com.quantexa.scoring.framework.spark.model.scores.CustomScore
import org.apache.spark.sql.{Dataset, SparkSession}

case class CreateCustomerScorecardInput(config: ProjectExampleConfig) extends CustomScore with WithOutputPath {

  def id = "MergeAllCustomerLevelScores"

  override def dependencies = (standardisedScores ++ mergedTransactionScores).toSet

  def outputDirectory: String = config.hdfsFolderScoring.toString
  def outputFileName: String = "CustomerScoreCardInput.parquet"
  override def outputFilePath: String = Paths.get(outputDirectory,outputFileName).toString
  def outputFilePathForUnderlying: String = Paths.get(outputDirectory,"CustomerScoreCardInputWithUnderlying.parquet").toString

  //Should output something of format CustomerScores

  val mergedTransactionScores: Seq[ScoreModel.Score with WithOutputPath] = Seq(MergeCustomerLevelTxnScores(config))
  val standardisedScores: Seq[ScoreModel.Score with WithOutputPath] = Seq(StandardiseCustomerLevelScores)
  val aggregatedTransactionsScorecardToCustomerScore: Seq[ScoreModel.Score with WithOutputPath]  = Seq(AggregatedTransactionScorecardToCustomerScore(config))

  private val inputFilePaths = mergedTransactionScores.map(_.outputFilePath) ++ standardisedScores.map(_.outputFilePath) ++ aggregatedTransactionsScorecardToCustomerScore.map(_.outputFilePath)

  override def score(spark: SparkSession)(implicit scoreInput: ScoreModel.ScoreInput): Any = {
    import spark.implicits._
    implicit val _ = spark

    val customerScoresWithUnderlying = inputFilePaths.map(spark.read.parquet(_).as[CustomerScores]).reduce(_ union _)

    val scorecardInputWithUnderlying = mergeScoreInputsByCustomerKey(customerScoresWithUnderlying)(spark)
    scorecardInputWithUnderlying.write.mode("overwrite").parquet(outputFilePathForUnderlying)

    val scorecardInputWithUnderlying2 = spark.read.parquet(outputFilePathForUnderlying).as[CustomerScores]
    val scorecardInputStandard = scoreCardInputToStandardisedBSO(scorecardInputWithUnderlying2)(spark)
    scorecardInputStandard.write.mode("overwrite").parquet(outputFilePath)
  }

  /**
    *
    * @param customerScoresWithUnderlying
    * @return A Dataset which contains all of the original fields on the scorecard input
    */
  private def mergeScoreInputsByCustomerKey(customerScoresWithUnderlying: Dataset[CustomerScores])(implicit spark:SparkSession) = {
    import spark.implicits._
    customerScoresWithUnderlying.groupByKey(x => (x.subject, x.keys)).
      mapGroups {
        case ((subjectId, keys), scoreCardInputIter) =>
          val scoresMap = scoreCardInputIter.map(_.customScoreOutputMap).reduce(_ ++ _)
          CustomerScores(subject = subjectId,
            keys = keys,
            customScoreOutputMap = scoresMap)
      }
  }

  /**
    * @param scorecardInputWithUnderlying
    * @return A Dataset compatible with the ScoreCard score type
    */
  private def scoreCardInputToStandardisedBSO(scorecardInputWithUnderlying: Dataset[CustomerScores])(implicit spark:SparkSession) = {
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
