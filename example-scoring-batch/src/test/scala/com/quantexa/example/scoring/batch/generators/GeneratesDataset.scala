package com.quantexa.example.scoring.batch.generators

import com.danielasfregola.randomdatagenerator.magnolia.RandomDataGenerator._
import org.apache.spark.sql.{Dataset, Encoder, SparkSession}
import org.scalacheck._

import scala.reflect.ClassTag


abstract class GeneratesDataset[C] {
  def generateRecords(n: Int)(implicit spark: SparkSession, ct: ClassTag[C], en:Encoder[C]): Dataset[C] = {
    implicit val arbitrary: Arbitrary[C] = Arbitrary(generate)
    import spark.implicits._

    random[C](n).toDS()
  }

  def generate(implicit ct: ClassTag[C]): Gen[C]
}
