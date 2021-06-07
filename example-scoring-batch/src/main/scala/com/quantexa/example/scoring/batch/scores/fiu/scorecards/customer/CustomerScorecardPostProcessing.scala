package com.quantexa.example.scoring.batch.scores.fiu.scorecards.customer

import java.nio.file.Paths

import com.quantexa.analytics.scala.scoring.model.ScoringModel.ScoreId
import com.quantexa.analytics.spark.scoring.ScoringUtils.WithOutputPath
import com.quantexa.example.scoring.batch.scores.fiu.integration.CreateCustomerScorecardInput
import com.quantexa.example.scoring.batch.scores.fiu.scorecards.{ScorecardExcelWriter, ScorecardOverallScore, ScoredCustomerDetail, ScoredUnderlyingScores}
import com.quantexa.example.scoring.model.fiu.RollupModel.CustomerScoreOutputWithUnderlying
import com.quantexa.example.scoring.model.fiu.ScorecardModel.{CustomerScores, ScoresWithContributions, CustomerScorecardOutputThin}
import com.quantexa.example.scoring.model.fiu.ScoringModel._
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.parameters.ScoreCardParameterValues
import com.quantexa.scoring.framework.scorecarding.ScoringModels
import com.quantexa.scoring.framework.spark.model.scores.CustomScore
import org.apache.spark.sql.functions.sort_array
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import com.quantexa.example.scoring.batch.model.fiu.ScorecardModels._

case class CustomerScorecardPostProcessing(config: ProjectExampleConfig) extends CustomScore with WithOutputPath {
  override def id: String = "CustomerScorecardPostProcessing"

  private val standardScorecard = CustomerScorecard(config)
  private val filePathForUnderlying = CreateCustomerScorecardInput(config).outputFilePathForUnderlying
  private val inputFilePath = standardScorecard.outputFilePath

  @transient
  private val scorecardExcelWriter = ScorecardExcelWriter(config)

  def outputDirectory: String = (Paths.get(config.hdfsFolderScoring) resolve Paths.get("ScoreCard")).toString

  def outputFileName: String = "CustomerScoreCardTransformed.parquet"

  override def outputFilePath: String = Paths.get(outputDirectory, outputFileName).toString

  def outputExcelFile =
    if (config.scorecardExcelFile.isDefined) {
      config.scorecardExcelFile.get
    } else if (System.getProperty("os.name").toLowerCase.contains("windows")) {
      System.getProperty("user.home") + "/tmp/" + "ScorecardOutput.xlsx"
    } else {
      Paths.get("./", "ScorecardOutput.xlsx").toString
    }

  override def score(spark: SparkSession)(implicit scoreInput: ScoreModel.ScoreInput): Any = {
    import spark.implicits._

    val scorecard = spark.read.parquet(inputFilePath).as[WeightedStandardisedScoreOutput]

    val underlyingInformation = spark.read.parquet(filePathForUnderlying).as[CustomerScores]

    val enrichedScorecardWithUnderlying: Dataset[CustomerScorecardOutputThin] = mapToNicerFormatWithUnderlyingScores(scorecard, underlyingInformation).cache()

    enrichedScorecardWithUnderlying.write.mode("overwrite").parquet(outputFilePath)

    val enrichedScorecardWithUnderlying2 = spark.read.parquet(outputFilePath)
    writeScorecardToFile(enrichedScorecardWithUnderlying.toDF())
  }

  private def mapToNicerFormatWithUnderlyingScores(scorecard: Dataset[WeightedStandardisedScoreOutput], underlyingInformation: Dataset[CustomerScores]): Dataset[CustomerScorecardOutputThin] = {
    import scorecard.sparkSession.implicits._
    scorecard.joinWith(underlyingInformation, scorecard("subjectId") <=> underlyingInformation("subject"), "left").
      map {
        case (scorecardOutput, underlyingScores) =>
          val (keys, underlyingScoresCustomMap) = createEmptyMapForNullJoin(scorecardOutput, underlyingScores)

          val scoreContributionsWithUnderyling: Map[ScoreId, ScoresWithContributions] = underlyingScores.customScoreOutputMap.map {
            case (scoreId, score) =>
              val weightedScore = getWeightedScore(scorecardOutput, scoreId)

              val scoreContributed = getContributingScore(scorecardOutput, scoreId)

              val candidateContribution = getCandidateContribution(weightedScore)

              val scoreParameterValues = getScoreParameterValues(weightedScore)

              scoreId -> createCustomerScoresWithContributions(score, candidateContribution, scoreContributed, scoreParameterValues)
          }.toMap

          CustomerScorecardOutputThin(scorecardOutput.subjectId.getOrElse(""), keys, Some(scorecardOutput.scorecardScore), scoreContributionsWithUnderyling)
      }
  }

  private def getScoreParameterValues(weightedScore: Option[ScoringModels.WeightedScore]): ScoreCardParameterValues = {
    weightedScore.map(_.scorecardParameters).get
  }

  private def getContributingScore(scorecardOutput: WeightedStandardisedScoreOutput, scoreId: ScoreId): Option[ScoringModels.WeightedScore] = {
    scorecardOutput.weightedScoreOutputMap.get(scoreId) match {
      case Some(s) if s.contributesToScorecardScore => Some(s)
      case _ => None
    }
  }

  private def getCandidateContribution(weightedScore: Option[ScoringModels.WeightedScore]): Double = {
    weightedScore.flatMap(_.candidateContribution).getOrElse(0.0)
  }

  private def getWeightedScore(scorecardOutput: WeightedStandardisedScoreOutput, scoreId: ScoreId): Option[ScoringModels.WeightedScore] = {
    scorecardOutput.weightedScoreOutputMap.get(scoreId)
  }

  private def createEmptyMapForNullJoin(scorecardOutput: WeightedStandardisedScoreOutput, underlyingScores: CustomerScores) = {
    underlyingScores match {
      case null => (CustomerKey(scorecardOutput.subjectId.getOrElse("")), Map.empty[String, CustomerScoreOutputWithUnderlying])
      case _ => (underlyingScores.keys, underlyingScores.customScoreOutputMap)
    }
  }

  private def createCustomerScoresWithContributions(score: CustomerScoreOutputWithUnderlying, candidateContribution: Double, scoreContributed: Option[ScoringModels.WeightedScore], scoreParameterValues: ScoreCardParameterValues) = {
    ScoresWithContributions(
      candidateContribution = candidateContribution,
      contribution = scoreContributed.map(_ => candidateContribution),
      scoreParameterValues = scoreParameterValues,
      severity = score.severity,
      band = score.band,
      description = score.description,
      underlyingScores = score.underlyingScores
    )
  }

  def writeScorecardToFile(df: DataFrame) = {
    import df.sparkSession.implicits._
    import org.apache.spark.sql.functions.{desc, explode, struct}
    val overallScore: Dataset[ScorecardOverallScore] = df.select($"subject", $"scorecardScore").orderBy(desc("scorecardScore")).as[ScorecardOverallScore]

    val explodedScoreContributions = df
      .select($"subject", $"key".as("scoreId"), $"scorecardScore", explode($"scoreContributions"))
      .cache()
    val restructuredUnderlyingScores = explodedScoreContributions.select(
      $"subject",
      $"scorecardScore",
      $"scoreId",
      explode($"value.underlyingScores").as("keyedBasicScoreOutput")
    ).withColumn(
      "keyedBasicScoreOutput",
      struct(
        $"keyedBasicScoreOutput.keyValues",
        $"keyedBasicScoreOutput.docType",
        struct(
          $"keyedBasicScoreOutput.severity",
          $"keyedBasicScoreOutput.band",
          $"keyedBasicScoreOutput.description"
        ).alias("basicScoreOutput")
      ))

    val underlyingScores: Dataset[ScoredUnderlyingScores] = restructuredUnderlyingScores
      .select(
        $"scoreId",
        $"subject",
        $"scorecardScore",
        sort_array($"keyedBasicScoreOutput.keyValues").as("keyValues"),
        $"keyedBasicScoreOutput.docType",
        $"keyedBasicScoreOutput.basicScoreOutput.severity",
        $"keyedBasicScoreOutput.basicScoreOutput.band",
        $"keyedBasicScoreOutput.basicScoreOutput.description"
      ).as[ScoredUnderlyingScores]
    val customerScores = explodedScoreContributions
      .select(
        $"subject",
        $"scorecardScore",
        $"key",
        $"value.candidateContribution",
        $"value.contribution",
        $"value.scoreParameterValues.group",
        $"value.severity",
        $"value.description"
      ).orderBy(
      desc("scorecardScore"),
      $"subject",
      $"group",
      desc("contribution"),
      desc("candidateContribution")).as[ScoredCustomerDetail]

    scorecardExcelWriter.writeScorecardToExcel(overallScore, customerScores, underlyingScores, outputExcelFile)
  }

}