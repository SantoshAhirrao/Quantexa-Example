package com.quantexa.example.scoring.batch.scores.fiu.templates

import com.quantexa.example.scoring.batch.utils.fiu.SparkTestScoringFrameworkTestSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExampleStdDevCalculationTests extends SparkTestScoringFrameworkTestSuite {

  import com.quantexa.example.scoring.batch.scores.fiu.templates.ExampleStdDevCalculation._
  import org.apache.spark.sql.functions._
  import spark.implicits._


  val name = "Standard Deviation Tests"

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
    result
  }


  it should "produce same population standard deviation as Spark, from large randomly generated set of single customer" in {

    // Parameters to generate sample dataset
    val numTransactionsPerDay:Int = 200
    val numberDays:Int = 30
    val numberCustomers: Int = 1
    val baseAmount:Double = 100
    val maxRange:Double = baseAmount * 0.3
    val toleranceDecimalPlaces = 5

    // Generate random sample dataset
    val generatedDates =  spark.sparkContext.range(0,numberDays).toDF("analysisDate").withColumn("analysisDate",$"analysisDate".cast("Int"))
      .withColumn("temp", explode(array((0 until numTransactionsPerDay).map(lit): _*))).drop("temp")
    val generatedDatesCustomers =  (1 to numberCustomers).map{i => generatedDates.withColumn("customerId",lit(i))  }.reduce(_ union _)
    val transactionDF = generatedDatesCustomers.
      withColumn("analysisAmount",  bround(rand(seed=5).alias("uniform") * maxRange + baseAmount,2)  ).as[MinimalTransaction]
    //transactionDF.show(50,false)

    // Test internal Spark standard deviation method against Quantexa method
    //println("\nstDevMethodSpark:")
    val stDevMethodSpark  = time {calcStDevMethodSpark(transactionDF)}
    //stDevMethodSpark.show()

    //println("\nstDevMethod1:")
    val stDevMethod1  = time {calculateStandardDeviationFromPartials(transactionDF) }
    //stDevMethod1.show()

    // Round final answers to specified number of decimal places (to neglect minor divergences between methods) and assert equality
    val stDevMethodSparkRounded = stDevMethodSpark.withColumn("standardDeviation",bround($"standardDeviation",toleranceDecimalPlaces))
    val stDevMethod1Rounded = stDevMethod1.withColumn("standardDeviation",bround($"standardDeviation",toleranceDecimalPlaces))
    assert(stDevMethodSparkRounded.except(stDevMethod1Rounded).union(stDevMethod1Rounded.except(stDevMethodSparkRounded)).count === 0)
  }

  it should "produce same population standard deviation as Spark, from a randomly generated set of skewed customer sizes" in {
    // Parameters to generate sample dataset
    val numTransactionsPerDay:Int = 200
    val numberDays:Int = 30
    val numberCustomers: Int = 5
    val baseAmount:Double = 100000
    val maxRange:Double = baseAmount * 0.3
    val toleranceDecimalPlaces = 5

    // Generate random sample dataset
    val generatedDates =  spark.sparkContext.range(0,numberDays).toDF("analysisDate").withColumn("analysisDate",$"analysisDate".cast("Int"))
      .withColumn("temp", explode(array((0 until numTransactionsPerDay).map(lit): _*))).drop("temp")
    val generatedDatesCustomers =  (1 to numberCustomers).map{i => generatedDates.withColumn("customerId",lit(i))  }.reduce(_ union _)
    val generatedDatesSingleCustomer = generatedDates.withColumn("customerId",lit(numberCustomers+1))
    val transactionDF = generatedDatesCustomers.union(generatedDatesSingleCustomer).
      withColumn("analysisAmount",  bround(rand(seed=5).alias("uniform") * maxRange + baseAmount,2)  ).as[MinimalTransaction]
    val transactionRepartitionedDF = transactionDF.repartition($"customerId")

    // Test internal Spark standard deviation method against Quantexa method
    //println("\nstDevMethodSpark:")
    val stDevMethodSpark  = time {calcStDevMethodSpark(transactionDF)}
    //stDevMethodSpark.show()

    //println("\nstDevMethod1:")
    val stDevMethod1  = time {calculateStandardDeviationFromPartials(transactionDF) }
    //stDevMethod1.show()

    // Round final answers to specified number of decimal places (to neglect minor divergences between methods) and assert equality
    val stDevMethodSparkRounded = stDevMethodSpark.withColumn("standardDeviation",bround($"standardDeviation",toleranceDecimalPlaces))
    val stDevMethod1Rounded = stDevMethod1.withColumn("standardDeviation",bround($"standardDeviation",toleranceDecimalPlaces))
    assert(stDevMethodSparkRounded.except(stDevMethod1Rounded).union(stDevMethod1Rounded.except(stDevMethodSparkRounded)).count === 0)
  }

}


