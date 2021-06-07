package com.quantexa.example.etl.projects.fiu.utils

import java.io.File
import java.sql.{Date, Timestamp}

import cats.data.Validated.{Invalid, Valid}
import com.quantexa.etl.validation.schema.{ReaderWithSchemaValidation, SchemaValidationError}
import org.apache.commons.io.FileUtils.deleteDirectory
import org.apache.log4j.Logger
import org.apache.spark.sql._
import org.iban4j
import org.apache.spark.sql.Encoder

import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

/*
  * Project ETL utilities/functions shared across input data sources
  */

object ProjectETLUtils {

  /***
    * Utility function allowing the conversion of CSV files to typed Parquet files
    * @param spark
    * @param logger
    * @param inputFile  Location of input csv file
    * @param destinationPath Desired location of Parquet file
    * @param saveMode Optional, Overwrite by default
    */

  def convertAndValidateCSV[T <: Product : Encoder](reader: ReaderWithSchemaValidation[T], inputPath: String, destinationFolder: String, spark: SparkSession, logger: Logger): Unit = {
    import spark.implicits._
    logger.info(s"Converting $inputPath")
    val outputPath = s"$destinationFolder/${inputPath.split('/').last.replace(".csv", ".parquet")}"
    reader.validate(reader.df(inputPath)) match {
      case Valid(df) =>
        logger.info(s"Writing $inputPath to: $outputPath")
        writeParquet(df.as[T], outputPath)
      case Invalid(e) => throw SchemaValidationError(s"Input data did not match schema: ${e.toString}")
    }
  }

  def outputDatasetToCSV[_](ds: Dataset[_], csvOutPath: String, saveMode: SaveMode = SaveMode.Overwrite): Unit = {
    //This function writes CSVs to storage and not on HDFS.
    ds
      .write
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .mode(saveMode)
      .csv("file:" + csvOutPath)
  }

  def deleteDir(dataRootDir: String): Unit = {
    Try(deleteDirectory(new File(dataRootDir))) match {
      case Success(v) => Success(v)
      case Failure(e) => println(e.getMessage)
    }
  }

  /*
    * Function to convert specified values to null
    */
  def valuesToNull(input: String, values: String*): Option[String] = {
    Option(input).flatMap(in => {
      if(values.contains(in trim)) None else Some(in)
    })
  }

  /*
  Function to convert java.sql.Timestamp to java.sql.Date
   */
  def timestampToDate(timestamp: Timestamp): Date = {
    Date.valueOf(timestamp.toLocalDateTime.toLocalDate)
  }

  /*
    * Function to validate if a given input string is a valid IBAN format
    */
  def isIBAN(input: String): Boolean = {
    try {
      iban4j.IbanUtil.validate(input)
      true
    } catch {
      case NonFatal(_) => false
    }
  }

  /*
     * Returns an (account,branchCode) tuple, if it can be extracted
     * from an input string. If not, the cleansed string is returned.
     * branchCode used here inter-changeably with sortCode.
     */
  def cleansedAccount(longAccount: String, ctry: Option[String]): (String, Option[String]) = {
    if (isIBAN(longAccount)) {
      val inputIBAN = iban4j.Iban.valueOf(longAccount)
      (inputIBAN.getAccountNumber, Some(inputIBAN.getBranchCode))
    } else if (ctry.contains("GB") && longAccount.length() == 14) {
      // For UK long form addresses, extract account number and sortCode
      (longAccount.substring(6), Some(longAccount.substring(0, 6)))
    } else if (ctry.contains("GB") && longAccount.length == 13) {
      ("0" + longAccount.substring(6), Some(longAccount.substring(0, 6)))
    } else
      (longAccount, None)
  }

  def readCSV(path: String, spark: SparkSession): DataFrame = spark
    .read
    .option("header", "true")
    .option("inferSchema", "true")
    .csv(path)

  def writeParquet[T](ds: Dataset[T], path: String): Unit = ds
    .write
    .mode(SaveMode.Overwrite)
    .parquet(path)
}
