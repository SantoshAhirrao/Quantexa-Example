package com.quantexa.example.scoring.batch.scores.fiu.facts

import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.scoring.batch.model.fiu.TransactionInputCustomScore
import com.quantexa.example.scoring.model.fiu.FactsModel.CustomerMonthFacts
import com.quantexa.example.scoring.model.fiu.ScoringModel.CustomerMonthKey
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, ScoreParameterIdentifier, ScoreParameters}
import frameless.Job
import org.apache.spark.sql.functions._
import org.apache.spark.sql._
import org.apache.spark.ml.knn.KNN
import org.apache.spark.ml.linalg
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.feature.StandardScaler
import org.apache.spark.ml.Pipeline
import com.quantexa.example.scoring.batch.utils.facts.Utils._

case class CalculateCustomerMonthFacts(config: ProjectExampleConfig, @transient
stats: CustomerDateStatsConfiguration = customermonth.TransactionValueStats.configuration
                                      ) extends FactAggregation[CustomerMonthFacts] with TransactionInputCustomScore with ScoreParameters{
  import CalculateCustomerMonthFacts._

  def id: String = "CustomerMonthFacts"

  def dateColumn: String = "monthsSinceEpoch"

  def parameters: Set[ScoreParameterIdentifier] = Set(ParameterIdentifier(None, "PastNumberOfMonthsForFactAggregation"))

  def processScoreableTransaction(spark: SparkSession, transactions: Dataset[ScoreableTransaction])(implicit scoreInput: ScoreInput): Unit = {

    import spark.implicits._

    val customerMonthFacts = calculateCustomerMonthFacts(transactions, stats).run
    customerMonthFacts.write.mode(SaveMode.Overwrite).parquet(config.hdfsFolderCustomerMonthFacts)
    val customerMonthFactsReread = spark.read.parquet(config.hdfsFolderCustomerMonthFacts).as[CustomerMonthFacts]

    val knnNeighbourResults = calculateKNeighbours(customerMonthFactsReread, K = 5)(spark).run
    knnNeighbourResults.write.mode(SaveMode.Overwrite).parquet(config.hdfsFolderCustomerKNNFacts)
    val knnNeighbourResultsReread = spark.read.parquet(config.hdfsFolderCustomerKNNFacts).as[KNNFacts]

    val calculatedNeighbourFacts = calculateNeighbourStats(knnNeighbourResultsReread, customerMonthFactsReread)(spark).run

    calculatedNeighbourFacts.write.mode(SaveMode.Overwrite).parquet(config.hdfsFolderCustomerMonthFactsWithNeighbourStats)

  }

  def fillDateGaps(transactions: Dataset[ScoreableTransaction], simpleAggregateStats: DataFrame)(implicit scoreInput: ScoreInput): DataFrame = {
    import transactions.sparkSession.implicits._

    val firstDateForTxnsOfInterest = lastCompleteMonthSinceEpoch - parameter[Int]("PastNumberOfMonthsForFactAggregation")

    val customersFirstTransactionDate = transactions.groupBy("customerId").agg(min(dateColumn).as("minDate"))

    val customersWithMaxMonth = customersFirstTransactionDate
      .withColumn("latestMonthToUse", lit(lastCompleteMonthSinceEpoch)).as[(String,Long,Long)]

    val customerDateWithNoGaps = customersWithMaxMonth.flatMap{
      case (customerId, minDate, maxDateOfAllTransactions) =>
        val minMonthToUse = if (minDate < firstDateForTxnsOfInterest) firstDateForTxnsOfInterest else minDate
        (minMonthToUse to maxDateOfAllTransactions).map(dt=>(customerId,dt))
    }.withColumnRenamed("_1","customerId")
      .withColumnRenamed("_2",dateColumn)

    customerDateWithNoGaps.join(simpleAggregateStats, Seq("customerId", dateColumn), "left")
  }

  protected def makeKeyColumn(df: DataFrame): DataFrame = {
    def udfKey = udf((cust:String, shiftedNumberMonthsSinceEpoch:Long) => {
      val date = java.sql.Date.valueOf(monthsSinceEpochToEndOfCalendarMonth(shiftedNumberMonthsSinceEpoch))
      CustomerMonthKey(cust, date)
      })

    def udfString = udf((cust:String, shiftedNumberMonthsSinceEpoch:Long) => {
      val date = java.sql.Date.valueOf(monthsSinceEpochToEndOfCalendarMonth(shiftedNumberMonthsSinceEpoch))
      CustomerMonthKey.createStringKey(cust, date)
      })

    df.withColumn("key", udfKey(col("customerId"), col(dateColumn))).
      withColumn("keyAsString", udfString(col("customerId"), col(dateColumn)))
  }

  def calculateCustomerMonthFactsStats(transactions: Dataset[ScoreableTransaction],
                                      stats: CustomerDateStatsConfiguration)(implicit scoreInput: ScoreInput): Job[DataFrame] = Job(
    calculateAllStats(transactions, stats)(scoreInput)
  )(transactions.sparkSession).withDescription(
    "Calculates statistics based on a scoreable transaction and outputs one row per customer per date (including non transacting days)"
  )

  def customerMonthFactsLikeToCustomerDateFacts(df: DataFrame): Job[Dataset[CustomerMonthFacts]] = Job(
    {
      import df.sparkSession.implicits._
      toDataset[CustomerMonthFacts](df)
    }
  )(df.sparkSession).withDescription("Selects columns for CustomerDateFacts and applies Dataset typing")

  def calculateCustomerMonthFacts(transactions: Dataset[ScoreableTransaction],
                                  stats: CustomerDateStatsConfiguration)(implicit scoreInput: ScoreInput): Job[Dataset[CustomerMonthFacts]] = (for {
    calculatedStats <- calculateCustomerMonthFactsStats(transactions, stats)(scoreInput)
    custDateFacts <- customerMonthFactsLikeToCustomerDateFacts(calculatedStats)
  } yield custDateFacts).withDescription("Calculates customer/date level statistics, including rolling statistics, from transactions, returning a Dataset[CustomerDateFacts]")

  /**
    * Returns a DataFrame of CustomerMonthKey plus the column of features and k nearest neighbours
    */
  protected def calculateKNeighbours(customerMonthFacts: Dataset[CustomerMonthFacts], K: Int)(implicit spark: SparkSession): Job[Dataset[KNNFacts]] = {
    import spark.implicits._

    Job(
      if(customerMonthFacts.limit(1).count > 0) {
      val (maxDate, minDate) = customerMonthFacts.agg(max($"key.calendarMonthEndDate"), min($"key.calendarMonthEndDate"))
        .as[(java.sql.Date, java.sql.Date)].take(1).head

      val customerMonthFactsWithFeatureVector = customerMonthFacts.map { cmf =>
        var features = Array(
          cmf.totalTransactionValueToDate,
          cmf.averageMonthlyTxnValueOver6MonthsExclude1RecentMonth.getOrElse(0D),
          cmf.minMonthlyTxnValueOver6MonthsExclude1RecentMonth.getOrElse(0D),
          cmf.maxMonthlyTxnValueOver6MonthsExclude1RecentMonth.getOrElse(0D))
        //This is required by the project example tests as the data generated is totally random and can result in very small number of customers in a given month with identical features (all zero). This causes an error.
        //Remove this line unless your tests are proven to fail without it.
        if (System.getProperty("scoreMode") == "test") features = features ++ Array(scala.math.random)

        (cmf.key.customerId, cmf.key.calendarMonthEndDate, Vectors.dense(features)
        )
      }.toDF("customerId", "calendarMonthEndDate", "features")

      calculateKNNMonthly(customerMonthFactsWithFeatureVector, minDate.toLocalDate, maxDate.toLocalDate, K = 5)

    } else spark.emptyDataset[KNNFacts]).withDescription(s"Calculating $K neighbours")
  }

  protected def calculateKNNMonthly(customerMonthFactsWithFeatureVector: DataFrame,
                                    minDate: java.time.LocalDate,
                                    maxDate: java.time.LocalDate,
                                    K: Int)
                                   (implicit spark: SparkSession): Dataset[KNNFacts] = {
    import spark.implicits._

    def doKNN(dfWithFeatureVector: DataFrame, K: Int): Dataset[KNNFacts] = {
      val knn = new KNN()
        .setFeaturesCol("scaledFeatures")
        .setAuxCols(Array("customerId", "calendarMonthEndDate"))
        .setTopTreeSize(scala.math.max(dfWithFeatureVector.count.toInt / 500, 1)) //This is the recommended default flexible tree size to use, see: https://github.com/saurfang/spark-knn
        .setBalanceThreshold(0)//This was added due to tests and project example subset data failing with small amounts of data (Tau going just below -1)

      if (System.getProperty("scoreMode") == "test") {
        knn.setBufferSizeSampleSizes((5 to 50 by 5).toArray) //This should be added for smaller data sets to avoid errors when estimating Tau
      }

      val model = knn.fit(dfWithFeatureVector).setK(K).setNeighborsCol("neighbours")
      model.transform(dfWithFeatureVector).as[KNNFacts]
    }

    (monthsSinceEpoch(minDate) to monthsSinceEpoch(maxDate))
      .foldLeft(spark.emptyDataset[KNNFacts]) { (dataFrameWithNeighbours, month) =>
        val filterForRelevantMonth = customerMonthFactsWithFeatureVector.filter(row => monthsSinceEpoch(row.getAs[java.sql.Date]("calendarMonthEndDate").toLocalDate) == month)

        //KNN won't work if only one customer in the month dataset.
        (filterForRelevantMonth.limit(2).count, dataFrameWithNeighbours.limit(1).count) match {
          case (0,_) | (1, _)  => dataFrameWithNeighbours
          case (_, knnFactsCount) =>
            val standardScaler = new StandardScaler()
              .setInputCol("features")
              .setOutputCol("scaledFeatures")
              .setWithStd(true)
              .setWithMean(true)

            val pipeline = new Pipeline().setStages(Array(standardScaler))
            val fitPipeline = pipeline.fit(filterForRelevantMonth)

            val scaledFeatureVector = fitPipeline.transform(filterForRelevantMonth)
            val monthlyNeighbours = doKNN(scaledFeatureVector, K)
            if (knnFactsCount == 0) // This if statement is necessary as Spark cannot union an empty Dataset to one with Rows
              monthlyNeighbours
            else
              dataFrameWithNeighbours.union(monthlyNeighbours)
        }
      }
  }

  protected def calculateNeighbourStats(knnResults: Dataset[KNNFacts], customerMonthFacts: Dataset[CustomerMonthFacts])(implicit spark: SparkSession): Job[DataFrame] = {
    Job({
      if(knnResults.limit(1).count > 0 && customerMonthFacts.limit(1).count > 0) {
        val customerToNeighbourLookup = knnResults.select(col("customerId"), col("calendarMonthEndDate"), explode(col("neighbours")).as("key"))

        val customerWithNeighbourStatistics = customerToNeighbourLookup
          .join(customerMonthFacts.select("key", "totalTransactionValueToDate"), Seq("key"))
          .groupBy("customerId", "calendarMonthEndDate").agg(
          avg("totalTransactionValueToDate").as("neighbourAverageTotalTransactionValue"),
          stddev("totalTransactionValueToDate").as("neighbourStddevTotalTransactionValue")
        ).select(struct("customerId", "calendarMonthEndDate").as("key"),
          col("neighbourAverageTotalTransactionValue"),
          col("neighbourStddevTotalTransactionValue"))

        customerMonthFacts
          .join(knnResults.select(struct("customerId", "calendarMonthEndDate").as("key"), col("neighbours.customerId").as("neighbours")), Seq("key"))
          .join(customerWithNeighbourStatistics, Seq("key"))
      } else spark.emptyDataFrame
    })(knnResults.sparkSession).withDescription("Calculating Neighbour Stats")
  }
}

object CalculateCustomerMonthFacts {

  case class KNNFacts(
                      customerId: String,
                      calendarMonthEndDate: java.sql.Date,
                      features: linalg.Vector,
                      scaledFeatures: linalg.Vector,
                      neighbours: Array[(String, java.sql.Date)]
                     )

}