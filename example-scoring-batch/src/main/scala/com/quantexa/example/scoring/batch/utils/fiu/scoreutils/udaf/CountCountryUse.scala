package com.quantexa.example.scoring.batch.utils.fiu.scoreutils.udaf

import org.apache.spark.sql.Row
import org.apache.spark.sql.expressions.{MutableAggregationBuffer, UserDefinedAggregateFunction}
import org.apache.spark.sql.types.{DataType, LongType, MapType, StructType, _}

//TODO: IP-201 Replace CombineMaps and CountCountryUse with CategoryCountUDAF
class CountCountryUse extends UserDefinedAggregateFunction {

  override def inputSchema: StructType = {
    new StructType().add("country", StringType, nullable = true)
  }

  override def bufferSchema: StructType = {
    new StructType().add("countCountryMap", MapType(StringType,LongType, true), nullable = true)
  }

  override def dataType: DataType = new MapType(StringType, LongType, true)

  override def deterministic: Boolean = true

  override def initialize(buffer: MutableAggregationBuffer): Unit = {
    buffer(0) = Map.empty[String,Long]
  }

  override def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    if(!input.isNullAt(0)) {
      import cats.implicits._

      val incomingCountry = Map(input.getAs[String](0)->1L)

      val updatedCountryMap = buffer.getMap[String,Long](0).toMap[String,Long] |+| incomingCountry

      buffer(0) = updatedCountryMap
    }
  }

  override def merge(buffer: MutableAggregationBuffer, row: Row): Unit = {
    import cats.implicits._

    buffer(0) = buffer.getMap[String,Long](0).toMap |+| row.getMap[String,Long](0).toMap
  }

  override def evaluate(buffer: Row): Any = {
    buffer.getMap[String,Long](0)
  }
}
