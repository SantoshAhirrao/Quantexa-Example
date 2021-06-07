package com.quantexa.example.graph.scripting.batch

import java.nio.file.{Files, Paths}

import com.quantexa.example.graph.scripting.GraphScriptingConfig
import com.quantexa.example.graph.scripting.batch.encoders.DataEncoders.clientResponseEntityGraphEncoder
import com.quantexa.graph.script.client.ClientResponseWrapper
import com.quantexa.resolver.core.EntityGraph.{DocumentId, EntityGraph}
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import org.apache.log4j.Logger
import io.circe.config.syntax._
import io.circe.generic.auto._
import org.apache.spark.sql.SaveMode.Overwrite
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.annotation.tailrec

// Not currently used. For suggested use on projects


/* This script runs OneHopNetworks on an input dataset, and retries any errors a configurable number of times. Intermediate
* successes and failures are written out. At each iteration, the intermediate successes written out do not include successful
* attempts from previous stages, i.e. only errors are retried. At the end of the script, all successes are collected and written
* to the normal output location for OneHopNetworks. */
object RunScriptWithRetries extends TypedSparkScript[GraphScriptingConfig] {

  def name = "RunScriptWithRetries"

  val fileDependencies = Map.empty[String, String]
  val scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession,
          log: Logger,
          args: Seq[String],
          config: GraphScriptingConfig,
          etlMetricsRepository: ETLMetricsRepository): Unit = {

    assert(config.oneHopStage.isDefined, "Config is missing for CustomerOneHopExpansionStage.")

    import spark.implicits._

    val oneHopConfig = config.oneHopStage.get

    val numberOfAttempts = oneHopConfig.datasetRetries.getOrElse(3)
    //Path to final output
    val outputDatasetPath = oneHopConfig.outputPath
    //Directory where intermediate successes/failures are written
    val intermediateStepsRoot = s"${outputDatasetPath}../customerOneHopAttempts/"
    //Location where overall output (i.e. collection of all successes) is written

    @tailrec
    def runScript(config: GraphScriptingConfig, attempts: Int): Unit = {
      val attemptNumber = numberOfAttempts - attempts + 1
      log.info(s"Attempt Number: $attemptNumber")
      log.info(s"Running script with input dataset: ${oneHopConfig.inputPath}")

      //This script will write the output to outputDatasetPath. It will overwrite the previous output (if any), which is okay
      // since the output of intermediate steps are saved to the intermediateStepsRoot directory.
      OneHopNetworks.run(spark, log, args, config, etlMetricsRepository)

      //Read output from script and gather successes/failures
      val outputDS = spark.read.parquet(outputDatasetPath).as[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]]
      val errorsDS = outputDS.filter(_.errorMessage.isDefined)
      val successesDS = outputDS.filter(_.errorMessage.isEmpty)

      val errorCount = errorsDS.count
      log.info(s"Script ran with $errorCount errors.")

      //Write successes/failures to intermediate location
      errorsDS.write.mode(Overwrite).parquet(s"${intermediateStepsRoot}errors-run-$attemptNumber.parquet")
      successesDS.write.mode(Overwrite).parquet(s"${intermediateStepsRoot}successes-run-$attemptNumber.parquet")

      //Create new input for next run. The new input is the dataset of all errors from the previous run.
      errorsDS.map(_.key).write.mode(Overwrite).parquet(s"${intermediateStepsRoot}nextInput.parquet")
      //Update config to use new input path so that we can recursively call runScript.
      val newOneHopConfig = oneHopConfig.copy(inputPath = s"${intermediateStepsRoot}nextInput.parquet")
      val newConfig = config.copy(oneHopStage = Some(newOneHopConfig))

      if(attempts > 0 && errorsDS.count > 0) {
        log.info("Moving on to next attempt...")
        runScript(newConfig, attempts - 1)
      }
    }

    def getDatasetIfExists(path: String): Dataset[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]] = {
      if(Files.exists(Paths.get(path))) {
        spark.read.parquet(path).as[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]]
      } else Seq.empty[ClientResponseWrapper[DocumentId, DocumentId, EntityGraph]].toDS
    }

    runScript(config, numberOfAttempts)

    //This concatenates together the datasets of all successes
    val allSuccesses = (1 to numberOfAttempts).map(i => getDatasetIfExists(s"${intermediateStepsRoot}successes-run-$i.parquet"))
      .reduce(_.union(_))

    //This gets all errors that have not been resolved by retries
    val remainingErrors = getDatasetIfExists(s"${intermediateStepsRoot}errors-run-$numberOfAttempts.parquet")

    //Concatenate the successes and remaining failures and write the output to the usual location
    allSuccesses.union(remainingErrors).write.mode(Overwrite).parquet(outputDatasetPath)

  }
}
