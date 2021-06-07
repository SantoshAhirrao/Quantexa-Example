package com.quantexa.example.scoring.batch.utils.fiu

import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.scoring.batch.scores.fiu.facts.{CalculateCustomerDateFacts, CustomerDateStatsConfiguration}
import com.quantexa.example.scoring.batch.scores.fiu.facts.customerdate.ScoreableTransactionTestData.{customerATransactions, customers_A_B_Transactions}
import com.quantexa.example.scoring.batch.utils.fiu.RunScoresInSparkSubmit.readParameters
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{IntegerScoreParameter, ParameterIdentifier}

import scala.reflect.ClassTag
import org.apache.spark.sql.{DataFrame, Dataset, Encoder}
import org.apache.spark.sql.functions.col


trait CustomerDateFactsTestSuite extends SparkTestScoringFrameworkTestSuite{

  import spark.implicits._

  def statisticsConfiguration : CustomerDateStatsConfiguration

   val custDateFacts = CalculateCustomerDateFacts(
    config = ProjectExampleConfig.default("/").copy(runDate = "2018-01-08"),
    stats = statisticsConfiguration
  )

  val parameterFile = custDateFacts.config.scoreParameterFile
  val parameterProvider = readParameters(parameterFile)

  implicit val scoreInput: ScoreInput = ScoreInput.empty.copy(
    parameters = Map(
      ParameterIdentifier(None, "PastNumberOfMonthsForFactAggregation") -> parameterProvider.parameters(ParameterIdentifier(None, "PastNumberOfMonthsForFactAggregation"))
    )
  )

  val singleCustomerActualOutput = custDateFacts.calculateCustomerDateFactsStats(
    customerATransactions.toDS(), statisticsConfiguration
  ).run().drop("keyAsString", "customerId", "analysisDate", "dateTime", "customerName").cache

  val twoCustomerActualOutput = custDateFacts.calculateCustomerDateFactsStats(customers_A_B_Transactions.toDS, statisticsConfiguration).run().
    drop("keyAsString", "customerId", "analysisDate", "dateTime", "customerName").cache

  val expectedOutputFields = (statisticsConfiguration.customerDateLevelStats ++ statisticsConfiguration.rollingStats).keys.toSet + "key"

  def assertExpectedStatsEqualToActualStats[T <: Product : Encoder: ClassTag](customerId: String, expectedDataset: Dataset[T],
                                                                              actualOutput: DataFrame, dateRanges: Map[String, String]): Unit = {
    val expectedColumns = expectedDataset.columns.map(col)

    val actualOutputArray = actualOutput.
      filter($"customerId" === customerId).
      orderBy($"analysisDate").
      select(expectedColumns :_*).
      as[T]

    val resultsInDateRange = dateRanges.flatMap{ case (startDate, endDate) =>
      actualOutputArray.filter($"analysisDate" >= java.sql.Date.valueOf(startDate) && $"analysisDate" <= java.sql.Date.valueOf(endDate)).collect
    }.toArray

    val actualOutputDS = spark.createDataset(resultsInDateRange)

    //Compare datasets with tolerance - this is because we are calculating numbers like standard deviation which have arbitrarily many decimal places, and it makes it significantly easier to produce expected test data by using a tolerance.
    assertDatasetApproximateEquals[T](expectedDataset, actualOutputDS, 0.000001)
  }

  def checkStatsForCustomer[T <: Product : Encoder: ClassTag](customerId: String, inputDataset: Dataset[ScoreableTransaction],
                                                              config: CustomerDateStatsConfiguration, expectedDataset : Seq[T])(implicit scoreInput: ScoreInput) = {
    val actualJob: DataFrame = custDateFacts.calculateCustomerDateFactsStats(inputDataset, config).run()
      .drop("keyAsString", "customerId", "analysisDate", "dateTime", "customerAccountCountry", "counterPartyAccountCountry")
    val value = actualJob.
      filter($"customerId" === customerId).
      orderBy($"analysisDate".desc)
    val actualDataset: Dataset[T] = value.as[T].orderBy($"key.analysisDate".desc)

    assertDatasetApproximateEquals[T](expectedDataset.toDS, actualDataset, 0.000001)
  }

}
