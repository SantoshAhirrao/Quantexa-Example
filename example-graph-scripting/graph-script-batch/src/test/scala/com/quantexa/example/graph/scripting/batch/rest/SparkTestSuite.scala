package com.quantexa.example.graph.scripting.batch.rest

import com.quantexa.graph.script.tags.GraphScriptTest
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

@GraphScriptTest
class SparkTestSuite extends FlatSpec with Matchers with BeforeAndAfterAll {

  //Spark will shut itself down when it detects the JVM shutdown; no need to use afterAll to close the SparkSession
  lazy val spark: SparkSession = SparkSession
    .builder()
    .master("local[*]")
    .appName("Graph Script Test")
    .config("spark.sql.shuffle.partitions", "1")
    .getOrCreate() // This won't create a session if there is one already in the current thread

  override def beforeAll() = {
    super.beforeAll()
    Logger.getLogger("org").setLevel(Level.ERROR)
  }

}