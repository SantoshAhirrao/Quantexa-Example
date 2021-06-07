package com.quantexa.example.etl.projects.fiu.utils

import java.io.File

import com.quantexa.example.etl.projects.fiu._
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success, Try}

object SharedTestUtils {

  def dataCleanUp(rootDir: String): Unit = {
    removeDirectory(s"$rootDir/run_1/raw/parquet")
    removeDirectory(s"$rootDir/run_1/DocumentDataModel")
    removeDirectory(s"$rootDir/run_1/Compounds")
    removeDirectory(s"$rootDir/run_2/raw/parquet")
    removeDirectory(s"$rootDir/run_2/DocumentDataModel")
    removeDirectory(s"$rootDir/run_2/Compounds")
    removeDirectory(s"$rootDir/metadata.parquet")

    def removeDirectory(directoryName: String): Unit = {
      Try(FileUtils.deleteDirectory(new File(directoryName))) match {
        case Success(v) => Success(v)
        case Failure(e) => println(e.getMessage)
      }
    }
  }

  def createLoaderConfig(rootDir: String, indexName: String, entityTypes: Seq[String] = Seq()): Config = {

    //Required for elastic deleter script
    val entityTypesString = entityTypes.map(str => "\"" + str + "\"").mkString(",")

    ConfigFactory.parseString(
      s"""
      metadataPath: "$rootDir/metadata.parquet"
      hdfsRoot: "$rootDir"
      elasticNodes : {
        searchNodes = ["${ElasticTestClient.host}:${ElasticTestClient.port}"]
        resolverNodes = ["${ElasticTestClient.host}:${ElasticTestClient.port}"]
      }
      entityTypes = [$entityTypesString]
      index {
        name = "$indexName"
      }
      """)
  }

  object ETLConfig {
    def apply(dataRoot: String, scoringLookups: Option[ScoringLookups], runId: String = ""): ETLConfig = {

      val customerInputFiles = CustomerInputFiles(s"$dataRoot/customer/raw/csv/customer.csv",
        s"$dataRoot/customer/raw/csv/account.csv",
        s"$dataRoot/customer/raw/csv/acc_to_cus.csv")
      val hotlistInputFile = HotlistInputFile(s"$dataRoot/hotlist/raw/csv/hotlist.csv")
      val transactionInputFile = TransactionInputFile(s"$dataRoot/transaction/raw/csv/transactions.csv",
        s"$dataRoot/customer/raw/parquet/acc_to_cus.parquet")

      val customer = DocumentConfig[CustomerInputFiles]("customer", dataRoot, customerInputFiles, runId)
      val hotlist = DocumentConfig[HotlistInputFile]("hotlist", dataRoot, hotlistInputFile, runId)
      val transaction = DocumentConfig[TransactionInputFile]("transaction", dataRoot, transactionInputFile, runId)

      new ETLConfig(customer, hotlist, transaction, scoringLookups)
    }
  }

  object DocumentConfig {
    def apply[A](documentType: String, dataRoot: String, rawCsvFiles: A, runId: String): DocumentConfig[A] = {

      val baseFileSystemPath = s"$dataRoot/$documentType/$runId"

      new DocumentConfig[A](documentType = documentType,
        inputFiles = rawCsvFiles,
        runId = Some(runId),
        isRunIdSetInConfig = Some(true),
        datasourceRoot = s"$dataRoot/$documentType",
        parquetRoot = s"$baseFileSystemPath/raw/parquet",
        validatedParquetPath = s"$baseFileSystemPath/validated/parquet",
        caseClassPath = s"$baseFileSystemPath/DocumentDataModel/DocumentDataModel.parquet",
        cleansedCaseClassPath = s"$baseFileSystemPath/DocumentDataModel/CleansedDocumentDataModel.parquet",
        metadataPath = s"$dataRoot/$documentType/metadata.parquet",
        aggregatedCaseClassPath = Some(s"$baseFileSystemPath/DocumentDataModel/AggregatedCleansedDocumentDataModel.parquet"))
    }
  }
}