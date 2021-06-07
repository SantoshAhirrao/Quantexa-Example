package com.quantexa.example.scoring.batch.scores.fiu.facts

import com.github.mrpowers.spark.daria.sql.DataFrameHelpers

import scala.annotation.tailrec
import scala.reflect.runtime.universe.TypeTag
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import com.quantexa.analytics.scala.Utils.getFieldNames
import com.quantexa.example.model.fiu.transaction.scoring.ScoreableTransaction
import com.quantexa.example.model.fiu.utils.DateModel.generateDateRange
import com.quantexa.scoring.framework.model.ScoreModel.ScoreInput
import org.apache.spark.sql.expressions.UserDefinedFunction
import com.quantexa.scoring.framework.parameters.{DateScoreParameter, ParameterIdentifier, StringScoreParameter}

//This must extend Serializable to prevent errors such as the following on implementation of the abstract class:
//  java.io.InvalidClassException: com.quantexa.example.scoring.batch.scores.fiu.facts.CalculateCustomerDateFacts; no valid constructor
abstract class FactAggregation[T<:Product:TypeTag] extends Serializable {
  /* Spark Column Name of the date field */
  def dateColumn:String

  /**
    * Creates a key column using the input DataFrame
    */
  protected def makeKeyColumn(df: DataFrame): DataFrame

  /**
    * For each customer in the simpleAggregateStats DataFrame, creates rows of data at corresponding timesteps
    * between their transactions.
    */
  protected def fillDateGaps(transactions: Dataset[ScoreableTransaction], simpleAggregateStats: DataFrame)(implicit scoreInput: ScoreInput):DataFrame

  private[facts] def calculateAllStats(transactions: Dataset[ScoreableTransaction],
                                       configuration: CustomerDateStatsConfiguration,
                                       addCustomStatistic: DataFrame => DataFrame = identity)(implicit scoreInput: ScoreInput): DataFrame = {

    val simpleAggregateStats = addSimpleAggregations(transactions, configuration.customerDateLevelStats)

    val simpleAggregateStatsFilledDates1 = fillDateGaps(transactions, simpleAggregateStats)(scoreInput)

    DataFrameHelpers.validatePresenceOfColumns(simpleAggregateStatsFilledDates1, Seq("customerId", dateColumn) ++ configuration.customerDateLevelStats.keySet)

    val simpleAggregateStatsFilledDates2 = addLongDateTime(simpleAggregateStatsFilledDates1)

    val simpleAggregateStatsFilledDates3 = fillMissingStatistics(simpleAggregateStatsFilledDates2)

    val complexAggregateStats = addRollingStatistics(simpleAggregateStatsFilledDates3, configuration.rollingStats)

    val complexAggregateStatsWithKey = addCustomStatistic(makeKeyColumn(complexAggregateStats))

    setNullabilityAndColumnOrder[T](complexAggregateStatsWithKey)
  }

  /**
    * @param desiredOrdering - Desired ordering of fields, can contain more fields than those that will be ordered
    * @param unorderedCols      - Columns that need to be ordered
    * @param orderedOutputCols  - Tail recursive parameter. On end of recursion will contain the ordered fields.
    *
    * @return shuffled version of unorderedCols where any string which appears in the desired ordering will be in the same ordering, with any extra fields at the end of this list.
    */
  @tailrec
  final def setColumnOrder(desiredOrdering: List[String], unorderedCols: List[String], orderedOutputCols: List[String] = List.empty[String]): List[String] = {
    desiredOrdering match {
      case Nil => orderedOutputCols ++ unorderedCols
      case head :: tail => unorderedCols.toSet(head) match {
        case true => setColumnOrder(tail, unorderedCols.filter(_ != head), orderedOutputCols :+ head)
        case false => setColumnOrder(tail, unorderedCols, orderedOutputCols)
      }
    }
  }

  /**
    * @param desiredOrderingAndNullability - Desired ordering of fields and nullability, can contain more fields than those that will be ordered
    * @param unorderedCols      - Fields that need to be ordered
    * @param orderedOutputCols  - Tail recursive parameter. On end of recursion will contain the ordered fields.
    *
    * @return shuffled version of unorderedCols where any string which appears in the desired ordering will be in the same ordering, with any extra fields at the end of this list.
    */
  @tailrec
  final def setColumnOrderAndNullability(desiredOrderingAndNullability: Seq[StructField],
                                   unorderedCols: Seq[StructField],
                                   orderedOutputCols: Seq[StructField] = Seq.empty[StructField]): StructType = {
    desiredOrderingAndNullability.toList match {
      case Nil => StructType(orderedOutputCols ++ unorderedCols)
      case head :: tail => unorderedCols.map(x=>x.name).toSet(head.name) match {
        case true => setColumnOrderAndNullability(tail, unorderedCols.filter(_.name != head.name), orderedOutputCols :+ head)
        case false => setColumnOrderAndNullability(tail, unorderedCols, orderedOutputCols)
      }
    }
  }

  /**
    * This sets the nullability of fields according to the case class that will (eventually) be output.
    * It does not require every field to be on the input dataframe, nor require every field present be output,
    * allowing for ease of testing. Fields which are not on the output case class T will retain their (non) nullability
    */
  def setNullabilityAndColumnOrder[T <: Product : TypeTag](df: DataFrame): DataFrame = {
    val customerDateFactSchema = Encoders.product[T].schema
    val correctedSchema = setColumnOrderAndNullability(customerDateFactSchema, df.schema.toSeq)

    val reorderSelectStatement = correctedSchema.map(x=>col(x.name))

    df.sparkSession.createDataFrame(df.select(reorderSelectStatement :_*).rdd, correctedSchema)
  }

  val datesBetween: UserDefinedFunction = udf { (minDate: java.sql.Date, maxDate: java.sql.Date) =>
    generateDateRange(minDate, maxDate)
  }

  val longsBetween: UserDefinedFunction = udf { (minLong: Long, maxLong: Long) => minLong to maxLong }

  //TODO: need to add support for multiple categories for generic category counter aggregation. e.g. pairs of countries.
  // see IP-733  https://quantexa.atlassian.net/browse/IP-733
  private def getEmptyDataTypeValue(dataType: DataType): Any  =  dataType match {
    case _:IntegerType => 0
    case _:FloatType => 0f
    case _:StringType => ""
    case _:DecimalType  => 0d
    case _:LongType  => 0l
    case ArrayType(elemType: DataType, _) =>
      Array.empty
    case MapType(keyType: DataType, valueType: DataType, _) =>
      Map.empty
    case _:BooleanType  => false
    case _:DateType  => new java.sql.Date(0L)
  }

  /**
    * Changes nulls in the DataFrame to their corresponding typed 'empty' element.
    * e.g. A Column with Type Option[Long] is changed to 0.
    */
  private[facts] def fillMissingStatistics(df: DataFrame): DataFrame = {

    val statsColumns = df.schema.filterNot {
      case StructField(name, dataType, nullable, metadata) => Set("customerId", dateColumn)(name)
    }.map(x => (x.name, x.dataType))

    def fillData(data: DataFrame, columnName: String, default: Column): DataFrame = {
      data.withColumn(s"${columnName}Tmp", coalesce(col(columnName), default)).drop(columnName).withColumnRenamed(s"${columnName}Tmp", columnName)
    }
    type StatsColumn = (String, DataType)

    val filledData = statsColumns.foldLeft(df) {
      (data: DataFrame, statsColumn : StatsColumn) =>
        statsColumn match {
          // this captures the structure for generic category counter aggregation
          case (columnName, StructType(Array(StructField(_,MapType(keyType,valueType,_),_,_)))) =>
            fillData(data, columnName,
              struct(typedLit(Map.empty[String,Long]).as("holder")))
          // this provides the default Map[String,Long].empty for Maps
          case (columnName, MapType(_, _, _)) =>
            fillData(data, columnName, typedLit(Map.empty[String,Long]))
          //this provides the default null for string values.
          case (columnName, StringType ) =>
            fillData(data, columnName, lit(null))
          //this provides the default 0 for numeric values (catch all).
          case (columnName, _ ) =>
            fillData(data, columnName, lit(0))}
    }
    filledData
  }

  /**
    * Adds the number of seconds since Epoch column named 'dateTime'
    * to the input DataFrame
    */
  private[facts] def addLongDateTime(df: DataFrame): DataFrame = {
    def getUnixTimeUDF = udf((date: java.sql.Date) => date.getTime / 1000)

    df.schema.find(x=>x.name == dateColumn).get match {
      case StructField(_,LongType,_,_) => df.withColumn("dateTime", col(dateColumn))
      case StructField(_,DateType,_,_) => df.withColumn("dateTime", getUnixTimeUDF(col(dateColumn)))
      case _ => throw new IllegalArgumentException(s"Type not supported for $dateColumn. Use Long or Date")
    }
  }
  /**
    * @param df DataFrame to apply rolling statistics to
    * @param rollingStats A Map of a new Column name to Column calculation
    * Applies the rolling statistics configured in the CustomerDateStatsConfiguration to the DataFrame
    */
  private[facts] def addRollingStatistics(df: DataFrame, rollingStats: Map[String, Column]): DataFrame = {
    rollingStats.foldLeft(df) {
      case (outDf, (colName, stat)) => {
        outDf.withColumn(colName, stat)
      }
    }
  }

  /**
    * Groups the transaction Dataset by customerId and the dateColumn specified. Aggregate Statistics are
    * calculated on these groups, producing one row per day the customer has transacted.
    */
  private[facts] def addSimpleAggregations(transactions: Dataset[ScoreableTransaction],
                                           aggregations: collection.Map[String, Column]) = {
    val customerDateLevelStats2 = aggregations.map {
      case (name, stat) => stat.as(name)
    }.toSeq

    // TODO: Implement logic that takes the most frequent name listed on a customer transaction for each date

    val additionalAggregationColumns = Seq(max("customerName").as("customerName" ))

    val statsWithAdditionalColumns = additionalAggregationColumns ++ customerDateLevelStats2

    DataFrameHelpers.validatePresenceOfColumns(transactions.toDF, Seq(dateColumn))
    aggregations.size match {
      case 0 => throw new IllegalArgumentException("Please specify at least one statistic to calculate at customer date level")
      case _ => transactions.groupBy("customerId", dateColumn).agg(statsWithAdditionalColumns.head, statsWithAdditionalColumns.tail: _*)
    }
  }
}
