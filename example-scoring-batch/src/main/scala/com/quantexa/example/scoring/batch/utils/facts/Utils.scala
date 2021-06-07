package com.quantexa.example.scoring.batch.utils.facts

import com.quantexa.analytics.scala.Utils.getFieldNames
import org.apache.spark.sql.{DataFrame, Dataset, Encoder}
import org.apache.spark.sql.functions.col
import scala.reflect.runtime.universe.TypeTag

object Utils {
  def toDataset[T: TypeTag](df: DataFrame)(implicit a: Encoder[T]): Dataset[T] = {
    val columns = getFieldNames[T].map(x => col(x))
    df.select(columns: _*).as[T]
  }
}
