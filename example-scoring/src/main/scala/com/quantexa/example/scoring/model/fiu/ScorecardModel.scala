package com.quantexa.example.scoring.model.fiu

import com.quantexa.analytics.scala.scoring.model.ScoringModel.{KeyedBasicScoreOutput, ScoreId}
import com.quantexa.example.scoring.model.fiu.RollupModel.CustomerScoreOutputWithUnderlying
import com.quantexa.example.scoring.model.fiu.ScoringModel.{AggregatedTransactionScoreOutput, AggregatedTransactionScoreOutputKeys, AggregatedTransactionScoreOutputWithUnderlying, CustomerKey}
import com.quantexa.scoring.framework.parameters.ScoreCardParameterValues
import com.quantexa.scoring.framework.scorecarding.ScoringModels.GroupedScoreCardScore
import com.quantexa.resolver.ingest.Model.id
import com.quantexa.scoring.framework.model.LookupModel.{KeyAnnotation, LookupSchema}

object ScorecardModel {

  //Input format to Scorecard
  case class CustomerScores(subject: String,
                            keys: CustomerKey,
                            customScoreOutputMap: collection.Map[ScoreId, CustomerScoreOutputWithUnderlying])

  case class CustomerScorecardOutputWithUnderlyingScores(subject: String,
                                                         keys: CustomerKey,
                                                         scorecardScore: Option[Int],
                                                         weightedScoreOutputMap: Seq[GroupedScoreCardScore],
                                                         contributingScoresOutputMap: Seq[GroupedScoreCardScore],
                                                         customScoreOutputMap: collection.Map[String, CustomerScoreOutputWithUnderlying])

  case class CustomerScorecardOutputThin(subject: String,
                                         keys: CustomerKey,
                                         scorecardScore: Option[Double],
                                         scoreContributions: collection.Map[ScoreId, ScoresWithContributions])

  case class ScoresWithContributions(candidateContribution: Double,
                                     contribution: Option[Double],
                                     scoreParameterValues: ScoreCardParameterValues,
                                     severity: Option[Int],
                                     band: Option[String],
                                     description: Option[String],
                                     underlyingScores: collection.Seq[KeyedBasicScoreOutput])

  case class AggregatedTransactionScores(subject: String,
                                         keys: AggregatedTransactionScoreOutputKeys,
                                         customScoreOutputMap: collection.Map[ScoreId, AggregatedTransactionScoreOutputWithUnderlying])

  case class MergeAggregatedTransactionScores(subject: String,
                                              keys: AggregatedTransactionScoreOutputKeys,
                                              customScoreOutputMap: collection.Map[ScoreId, AggregatedTransactionScoreOutput])


  case class AggregatedTransactionScorecardOutput(subject: String,
                                                  keys: AggregatedTransactionScoreOutputKeys,
                                                  meanSeverity: Int)

  case class CustomerWithAggregatedTransaction(@id @KeyAnnotation customerId: String,
                                               scoredRelationshipWithUnderlying: Seq[AggregatedTransactionScorecardOutput],
                                               weightedScore: Int) extends LookupSchema{
  }
}
