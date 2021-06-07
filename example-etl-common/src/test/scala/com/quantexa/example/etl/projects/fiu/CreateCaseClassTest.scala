package com.quantexa.example.etl.projects.fiu

import com.quantexa.example.etl.projects.fiu.utils.SparkTestSession
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.scalatest.{FlatSpec, Matchers}

/***
  * This class was created to show an example of how to unit test with a local SparkSession
  */
class CreateCaseClassTest extends FlatSpec with Matchers {
  val spark: SparkSession = SparkTestSession.spark
  import spark.implicits._

  "functions.leftJoin" should "join the DataFrames given correctly" in {

    val leftDF = Seq(
      ("a",1)
      , ("a",2)
      , ("b",3)
    ).toDF("colA","colB")

    val rightDF = Seq(
      ("a",true)
      , ("b",false)
      , ("c",false)
    ).toDF("colA","colC")

    val expected = Seq(
      Row("a",1,true)
      , Row("a",2,true)
      , Row("b",3,false)
    )

    val result = functions.leftJoin(leftDF,rightDF,"colA")

    result.collect().toList  should contain theSameElementsAs expected
  }
}

/***
  * This should be the function you wish to test defined in your project code.
  */
object functions{
  def leftJoin(leftDF:DataFrame,rightDF:DataFrame,colToJoin:String):DataFrame = {
    leftDF.join(rightDF, Seq(colToJoin), "left_outer")
  }
}