package com.quantexa.example.scoring.batch.utils.fiu

import com.quantexa.example.scoring.model.fiu.ScoringModel.CustomerDateKey
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.types.StructType
import com.quantexa.analytics.test.ImportLiteGraphFromCSV.getLiteGraphWithId
import org.apache.poi.ss.usermodel.{Workbook, WorkbookFactory}

object TestDataUtils {

  /**
    * Loads JSON from file to a DataFrame
    *
    * @param cpPath Location of json to load on classpath.
    * @param schema Optionally apply a schema to the file to load - if not specified, Spark will infer the schema
    * @return
    */
  def loadJSON(spark: SparkSession, cpPath: String, schema: Option[StructType] = None): DataFrame = {
    // this only works if the file in cpPath is in the same project as the test
    val reader = schema.map(s => spark.read.schema(s)).getOrElse(spark.read)
    val dfWithNullableTrueSchema = reader.json(this.getClass.getResource(cpPath).getPath)
    spark.sqlContext.createDataFrame(dfWithNullableTrueSchema.rdd, schema.get)
  }

  object CombineMaps {
    type Counts = Map[String, Long]

    def combine(x: Counts, y: Counts): Counts = {
      val merged = x.toSeq ++ y.toSeq
      val grouped = merged.groupBy(_._1)
      val combined = grouped.mapValues(_.map(_._2).toList.sum)

      combined
    }
  }

}

case class CustomerTransactionCountStats(key:CustomerDateKey,
                                         numberOfTransactionsToday: Long,
                                         numberOfTransactionsToDate: Long,
                                         numberOfPreviousTransactionsToDate: Long,
                                         numberOfTransactionsOverLast7Days: Long,
                                         numberOfTransactionsOverLast30Days: Long,
                                         previousAverageTransactionVolumeToDate: Option[Double],
                                         previousStdDevTransactionVolumeToDate: Option[Double],
                                         previousAverageTransactionVolumeLast7Days: Option[Double],
                                         previousStdDevTransactionVolumeLast7Days: Option[Double],
                                         previousAverageTransactionVolumeLast30Days: Option[Double],
                                         previousStdDevTransactionVolumeLast30Days: Option[Double])
object CustomerTransactionCountStats {
  implicit def mandatoryToOption[T](a:T):Option[T] = Some(a)
}


case class CustomerTransactionValueStats(key:CustomerDateKey,
                                          totalValueTransactionsToday: Double,
                                         totalTransactionValueToDate : Double,
                                         totalPreviousTransactionValueToDate : Double,
                                          totalValueTransactionsOverLast7Days: Double,
                                          totalValueTransactionsOverLast30Days: Double,
                                          previousAverageTransactionAmountToDate: Option[Double],
                                          previousStdDevTransactionAmountToDate: Option[Double],
                                          previousAverageTransactionAmountLast7Days: Option[Double],
                                          previousStdDevTransactionAmountLast7Days: Option[Double],
                                          previousAverageTransactionAmountLast30Days: Option[Double],
                                          previousStdDevTransactionAmountLast30Days: Option[Double]
                                        )
object CustomerTransactionValueStats {
  implicit def mandatoryToOption[T](a:T):Option[T] = Some(a)
}

case class CustomerTransactionValueStdDev(key: CustomerDateKey,
                                          numberOfTransactionsToday: Long,
                                          numberOfTransactionsToDate: Long,
                                          totalValueTransactionsToday: Double,
                                          totalTransactionValueToDate: Double,
                                          transactionValueStdDevToDate: Option[Double],
                                          totalSquareTransactionValueToday: Double,
                                          totalSquareTransactionValueToDate: Option[Double],
                                          transactionValueVarianceToDate: Option[Double],
                                          customerDebitTransactionEMAOverLast30Days: Option[Double],
                                          customerDebitTransactionSMAOverLast30Days: Option[Double]
                                         )

object CustomerTransactionValueStdDev {
  implicit def mandatoryToOption[T](a:T):Option[T] = Some(a)
}
  
object LiteGraphTestData {
  private def inputPaths = Seq(getClass.getResource("/scoring/litegraphs/customerSettlingToRelatedThirdPartiesBusiness/customerSettlingRelatedCounterparties").getPath,
    getClass.getResource("/scoring/litegraphs/customerSettlingToRelatedThirdPartiesBusiness/settlingToSameBusinessViaMultipleRelationships").getPath,
    getClass.getResource("/scoring/litegraphs/customerSettlingToRelatedThirdPartiesBusiness/subsetOfCounterpartiesRelated").getPath,
    getClass.getResource("/scoring/litegraphs/customerSettlingToRelatedThirdPartiesBusiness/tradesLinkedBackToCustomer").getPath,
    getClass.getResource("/scoring/litegraphs/customerSettlingToRelatedThirdPartiesAddress/customerTradeRelationshipsCommonAddress").getPath,
    getClass.getResource("/scoring/litegraphs/customerSettlingToRelatedThirdPartiesAddress/relatedCounterpartiesLinkedByAccount").getPath)

  val data = inputPaths.map{ path =>
    val filename = path.split("/").last
    filename -> getLiteGraphWithId(path)
  }.toMap
}

object CustomerDateFactsTestData {
  private val fileName = "customerdate/ExpectedTestDataForCalculateCustomerDateFacts.xlsx"

  val workbook: Workbook = WorkbookFactory.create(getClass.getClassLoader.getResourceAsStream(fileName))
}