package com.quantexa.example.etl.projects.fiu.utils

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession

object SparkTestSession {
  val spark = SparkSession
    .builder()  
    .master("local[*]")
    .appName("Test app")
    .config("spark.default.parallelism","10")
    .getOrCreate()
  
  Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
  spark.sparkContext.setLogLevel("WARN")
}
