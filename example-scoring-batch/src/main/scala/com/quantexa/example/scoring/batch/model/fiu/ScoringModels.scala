package com.quantexa.example.scoring.batch.model.fiu

import org.apache.spark.sql.DataFrame
import com.quantexa.scoring.framework.model.ScoreModel.BasicScoreOutput
import com.quantexa.analytics.scala.scoring.model.ScoringModel.ScoreId

object ScoringModels {

  trait CreateFacts {
    def addFacts(df:DataFrame):DataFrame
  }

  /** Standard Scorecard format - recommend adding additional fields depending on use case such as customer name, line of business etc. */
  case class CustomerScorecard(customerId: String,
                               scoredOutputCollection: collection.Map[ScoreId, ScoredOutput],
                               score: Option[Double] = None) {
    /** Validates all scoredOutputCollections have been built correctly */
    def validate(): Unit = this.scoredOutputCollection.foreach {
      case (scoreName, scoredOutput) => scoredOutput.validate(scoreName)
    }
    //TODO: IP-520 Methods to facilitate reporting on scoring and viewing data (e.g. including but not exclusive to auto-excel export of this data).
  }

  /**
   * A single score which is at the same level as the scored entity in the scorecard,
   * this may contain information about underlying scores (e.g. from roll ups/roll downs)
   * */
  case class ScoredOutput(basicScoreOutput: BasicScoreOutput,
                          group: Option[String],
                          candidateContribution: Double,
                          contribution: Option[Double],
                          underlyingScores: Seq[KeyedBasicScoreOutput]) {
    def applyCandidateContribution: ScoredOutput = {
      this.copy(contribution = Some(candidateContribution))
    }
    /** Validates underlying scores (can be empty) */
    def validate(scoreName:ScoreId): Unit = {
      this.underlyingScores.foreach(_.validate(scoreName))
    }
  }

  /**A basic score output with lookup keys to original data*/
  case class KeyedBasicScoreOutput(keyValues: Seq[LookupKey], docType:String, basicScoreOutput: BasicScoreOutput) {
    /** Checks at least one keyValue exists and all are valid */
    def validate(scoreName:ScoreId): Unit = if (this.keyValues.isEmpty) throw new IllegalArgumentException(
      s"$scoreName : Please specify at least one key value which relates to the basic score output"
    ) else keyValues.foreach(_.validate(scoreName))
  }

  /**
   * A key value pair which can be used to lookup values in real time, that is compatible with Spark Serialisation.
   * Only populate ONE value
   * */
  case class LookupKey(name: String, stringKey: Option[String], dateKey: Option[java.sql.Date], intKey: Option[Int], longKey: Option[Long]) {
	  /** Checks exactly one key has been defined.*/
	  def validate(scoreName:ScoreId): Unit = if (this.stringKey.size + this.dateKey.size + this.intKey.size + this.longKey.size != 1) throw new IllegalArgumentException(s"$scoreName : Exactly one key must be defined for the lookup key $name")
  }

} 
 