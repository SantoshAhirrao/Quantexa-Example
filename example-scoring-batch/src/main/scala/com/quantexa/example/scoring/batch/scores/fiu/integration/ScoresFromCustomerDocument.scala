package com.quantexa.example.scoring.batch.scores.fiu.integration

import java.nio.file.Paths

import cats.data.NonEmptyList
import com.quantexa.analytics.scala.scoring.model.ScoringModel.ScoreId
import com.quantexa.analytics.scala.scoring.scoretypes.AugmentedDocumentScore
import com.quantexa.analytics.spark.scoring.ScoringUtils.WithOutputPath
import com.quantexa.analytics.spark.scoring.scoretypes.MergeDocumentScoresAndCalculateStatistics
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.model.fiu.RollupModel.CustomerScoreOutputWithUnderlying
import com.quantexa.example.scoring.model.fiu.ScoringModel._
import com.quantexa.example.scoring.scores.fiu.document.customer._
import com.quantexa.scoring.framework.model.ScoreModel
import com.quantexa.example.scoring.model.fiu.ScorecardModel.CustomerScores
import com.quantexa.scoring.framework.model.DataDependencyModel.DataDependencyProviderDefinitions
import com.quantexa.scoring.framework.spark.model.scores.CustomScore
import org.apache.spark.sql.SparkSession

case class MergeCustomerScores(ddpForStatisticsCalculation: Option[DataDependencyProviderDefinitions] = None)
  extends MergeDocumentScoresAndCalculateStatistics[Customer, CustomerScoreOutput, CustomerScoreOutputKeys](ddpForStatisticsCalculation) {
  def id: String = "MergeCustomerScores"

  override def dependencies = Set.empty
  
  def scores: NonEmptyList[AugmentedDocumentScore[Customer, CustomerScoreOutput]] = NonEmptyList.of(
    CancelledCustomer,
    NewCustomer,
    HighRiskCustomerDiscrete,
    CustomerAddressInLowValuePostcode,
    CustomerFromHighRiskCountry
  )

}

object StandardiseCustomerLevelScores extends CustomScore with WithOutputPath{

  def id = "AddUnderlyingScoresToCustomerLevelScores"

  def outputDirectory: String = MergeCustomerScores().outputDirectory

  def outputFileName: String = MergeCustomerScores().outputFileName.replaceAll("Merged$","Merged_ForScorecard")

  def outputFilePath: String = Paths.get(outputDirectory,outputFileName).toString

  def mergedDocumentScoresInput = MergeCustomerScores

  override def dependencies = Set(mergedDocumentScoresInput())
  private val inputFile = mergedDocumentScoresInput().outputFilePath

  def score(spark: SparkSession)(implicit scoreInput: ScoreModel.ScoreInput): Any = {
    import spark.implicits._

    val input = spark.read.parquet(inputFile).as[(String,CustomerScoreOutputKeys,Map[ScoreId,CustomerScoreOutput])]

    val addUnderlyingScores = input.map(x=>(x._1,x._2,x._3.mapValues{
      customerScoreOutput=>
        val addUnderlyingField = CustomerScoreOutputWithUnderlying(
          keys = CustomerKey(customerScoreOutput.keys.customerId),
          severity = customerScoreOutput.severity,
          band = customerScoreOutput.band,
          description = customerScoreOutput.description,
          underlyingScores = Seq.empty)
        addUnderlyingField
    }))
      .withColumnRenamed("_1","subject")
      .withColumnRenamed("_2","keys")
      .withColumnRenamed("_3","customScoreOutputMap")
      .as[CustomerScores]

    addUnderlyingScores.write.mode("overwrite").parquet(outputFilePath)
  }
}

