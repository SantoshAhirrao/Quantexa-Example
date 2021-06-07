package com.quantexa.example.scoring.batch.scores.fiu.scorecards.aggregatedTransaction

import java.nio.file.Paths

import com.quantexa.analytics.spark.scoring.ScoringUtils.WithOutputPath
import com.quantexa.example.scoring.batch.scores.fiu.integration.CreateAggregatedTransactionScorecardInput
import com.quantexa.example.scoring.model.fiu.ScorecardModel.{AggregatedTransactionScorecardOutput, AggregatedTransactionScores, CustomerScores, CustomerWithAggregatedTransaction}
import com.quantexa.example.scoring.model.fiu.ScoringModel.{AggregatedTransactionScoreOutputKeys, CustomerKey}
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.scoring.framework.spark.model.scores.CustomScore
import com.quantexa.example.scoring.batch.model.fiu.ScorecardModels._
import com.quantexa.example.scoring.model.fiu.RollupModel.CustomerScoreOutputWithUnderlying
import org.apache.spark.sql.functions.max
import org.apache.spark.sql.{Dataset, SparkSession}
import com.quantexa.example.scoring.batch.scores.fiu.scorecards.ScorecardUtils._

case class AggregatedTransactionScorecardToCustomerScore(config: ProjectExampleConfig) extends CustomScore with WithOutputPath {
  private val standardScorecard = AggregatedTransactionScorecard(config)
  private val filePathForUnderlying = CreateAggregatedTransactionScorecardInput(config).outputFilePathForUnderlying
  private val inputFilePath = standardScorecard.outputFilePath

  override def score(spark: SparkSession)(implicit scoreInput: ScoreModel.ScoreInput): Any = {
    import spark.implicits._

    val weights = Seq(100, 75, 50)

    val scorecard = spark.read.parquet(inputFilePath).as[WeightedStandardisedScoreOutput]
    val underlyingInformation = spark.read.parquet(filePathForUnderlying).as[AggregatedTransactionScores]
    val enrichedScorecardWithUnderlying = extractAggregatedTransactionsScorecardOutput(scorecard, underlyingInformation)
    val groupedAggregatedTransactionsByCustomer = groupAggregatedTransactionsByCustomer(enrichedScorecardWithUnderlying, weights)
    val aggregatedTransactionScorecardAsCustomerScore = aggregatedTransactionsScorecardToCustomerScore(groupedAggregatedTransactionsByCustomer, id)

    aggregatedTransactionScorecardAsCustomerScore.write.mode("overwrite").parquet(outputFilePath)
  }

  override def outputFilePath: String = Paths.get(outputDirectory, outputFileName).toString

  override def outputFileName: String = this.id + ".parquet"

  override def outputDirectory: String = (Paths.get(config.hdfsFolderScoring) resolve Paths.get("ScoreCard")).toString

  override def id: String = "AggregatedTransactionsScorecardToCustomerScore"

  override def dependencies: Set[ScoreModel.Score] = Set(AggregatedTransactionScorecard(config))


  /**
    * Transforms the grouped aggregated transaction per customer to the schema of the grouped customer scores.
    *
    * @param customerWeightedAggregatedTransactionsScore a Dataset[CustomerWithAggregatedTransactions] which holds score run output data for
    *                                          all aggregated transactions
    * @param aggregatedTransactionAsCustomerScoreScoreId the scoreId of aggregated transactions scorecard contribution to index it in the customerScoreOutputMap
    * @return
    */
  private def aggregatedTransactionsScorecardToCustomerScore(customerWeightedAggregatedTransactionsScore: Dataset[CustomerWithAggregatedTransaction],
                                                             aggregatedTransactionAsCustomerScoreScoreId: String): Dataset[CustomerScores] = {

    import customerWeightedAggregatedTransactionsScore.sparkSession.implicits._

    val maxAggregatedTransactionsScoreRow = customerWeightedAggregatedTransactionsScore.map(_.weightedScore).agg(max("value")).head

    customerWeightedAggregatedTransactionsScore.map {
      case CustomerWithAggregatedTransaction(customerId, aggregatedTransactionsScoreList, weightedScore) =>

        val normalisedSeverity = Some((weightedScore.toDouble * 100 / maxAggregatedTransactionsScoreRow.getInt(0)).toInt)
        val desc = Some(s"Customer has ${aggregatedTransactionsScoreList.size} aggregated transactions and the max aggregated transactions scored $weightedScore")
        val band = normalisedSeverity.map(defineNormalisedBand)

        CustomerScores(
          customerId,
          CustomerKey(customerId),
          Map(aggregatedTransactionAsCustomerScoreScoreId ->
            CustomerScoreOutputWithUnderlying(CustomerKey(customerId),
              normalisedSeverity,
              band,
              desc,
              Seq.empty
            )
          )
        )
    }
  }


  private def groupAggregatedTransactionsByCustomer(scoredAggregatedTransactions: Dataset[AggregatedTransactionScorecardOutput],
                                                    weights: Seq[Int]): Dataset[CustomerWithAggregatedTransaction] = {
    import scoredAggregatedTransactions.sparkSession.implicits._

    scoredAggregatedTransactions.groupByKey(_.keys.customerId).mapGroups {
      case (customerId, aggregatedTransactionsScorecardOutputIter) =>

        val aggregatedTransactionsScorecardOutputSeq = aggregatedTransactionsScorecardOutputIter.toSeq

        CustomerWithAggregatedTransaction(
          customerId,
          aggregatedTransactionsScorecardOutputSeq,
          calculateTopXAggregatedTransactionsScoresWeighted(
            aggregatedTransactionsScorecardOutputSeq.map(_.meanSeverity),
            weights.length,
            weights
          )
        )
    }
  }

  /**
    *
    * @param scoredAggregatedTransactions   a Seq[Int], holding the total severity scores for all customer's AggregatedTransactions
    * @param positiveAggregatedTransactions Int, holding the max number of relationships to weight
    * @param weights                        a decreasingly ordered Seq[Int], holding integers to weight customer's positiveAggregatedTransactions
    * @return                               a weighted sum Int of top positive AggregatedTransactions for customer
    */
  private def calculateTopXAggregatedTransactionsScoresWeighted(scoredAggregatedTransactions: Seq[Int],
                                                        positiveAggregatedTransactions: Int,
                                                        weights: Seq[Int]): Int = {

    val positiveScores = scoredAggregatedTransactions.filter(_ > 0).sortWith(_ > _)
    if (positiveAggregatedTransactions != weights.length) throw new IllegalArgumentException("positiveAggregatedTransactions and number of weights doesn't match")
    else positiveScores.zip(weights).map { case (score, weight) => score * weight / 100 }.sum

  }


  private def extractAggregatedTransactionsScorecardOutput(scorecard: Dataset[WeightedStandardisedScoreOutput],
                                                   underlyingInformation: Dataset[AggregatedTransactionScores]): Dataset[AggregatedTransactionScorecardOutput] = {
    import scorecard.sparkSession.implicits._
    scorecard.joinWith(underlyingInformation, scorecard("subjectId") === underlyingInformation("subject"),
      "left")
      .map {
        case (scorecardOutput, underlyingScores) =>
          val keys = getKeysForNullJoin(scorecardOutput, underlyingScores)

          val severities = scorecardOutput.weightedScoreOutputMap.flatMap {
            case (scoreName, score) =>
              score match {
                case s if s.contributesToScorecardScore => s.severity
                case _ => None
              }
          }.toSeq

          AggregatedTransactionScorecardOutput(
            scorecardOutput.subjectId.getOrElse(""),
            keys,
            severities.sum / severities.size)
      }
  }

  private def getKeysForNullJoin(scorecardOutput: WeightedStandardisedScoreOutput, underlyingScores: AggregatedTransactionScores) = {
    underlyingScores match {
      case null => AggregatedTransactionScoreOutputKeys(scorecardOutput.subjectId.getOrElse(""), "", "")
      case _ => underlyingScores.keys
    }
  }
}
