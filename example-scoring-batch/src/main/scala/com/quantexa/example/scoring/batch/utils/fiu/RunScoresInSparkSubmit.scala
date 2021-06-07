package com.quantexa.example.scoring.batch.utils.fiu

import com.quantexa.example.scoring.batch.model.fiu.{AggregatedTransactionsScorecardModelDefinition, CustomerScorecardModelDefinition, FiuBatchModelDefinition}
import com.quantexa.example.scoring.utils.TypedConfigReader.{ProjectExampleConfig, ScoreParameterFile}
import com.quantexa.scoring.framework.parameters.{CSVScoreParameterSource, ScoreParameterProvider}
import com.quantexa.scoring.framework.spark.execution.{SparkExecutor, SparkScoringContext}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.auto._
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession

object RunScoresInSparkSubmit extends TypedSparkScript[ProjectExampleConfig] {
  val name = "RunScoresInSparkSubmit"

  def fileDependencies: Map[String, String] = Map.empty[String, String]

  def scriptDependencies: Set[QuantexaSparkScript] = Set.empty[QuantexaSparkScript]

  def readParameters(parameterFilePath: ScoreParameterFile): ScoreParameterProvider = {
    val csvSource = new CSVScoreParameterSource(filePath = parameterFilePath.filePath, classpath = parameterFilePath.classpath)
    ScoreParameterProvider.fromSources(Seq(csvSource))
  }

  override def run(sparkSession: SparkSession,
                             logger: Logger,
                             args: Seq[String],
                             projectConfig: ProjectExampleConfig,
                             etlMetricsRepository: ETLMetricsRepository): Unit = {

    implicit val spark: SparkSession = sparkSession

    def runBatchScore =
      Option(System.getProperty("runBatchScore")).getOrElse(
        throw new IllegalStateException(
          """runBatchScore is not set, please set on by using -DrunBatchScore="CancelledCustomer" """))

    Option(System.getProperty("scoreOutputRoot")).getOrElse(
      throw new IllegalStateException(
        """scoreOutputRoot is not set, please set by using -DscoreOutputRoot="/user/quantexa/someScoringLocation/" """))

    logger.info(s"NOTE THAT SCORING OUTPUT SET TO ${System.getProperty("scoreOutputRoot")}")

    val parameterFilePath = projectConfig.scoreParameterFile
    val parameterProvider = readParameters(parameterFilePath)

    val fiuBatchModelDefinition = FiuBatchModelDefinition(projectConfig)

    val fiuPhaseContext = SparkScoringContext(sparkSession,
      fiuBatchModelDefinition,
      parameterProvider,
      fiuBatchModelDefinition.dataDependencyProviderDefinitions)

    def runner(batchScore: String): Any = {
      batchScore.toLowerCase() match {
        case "all" => {
          val allStages = Seq(
            "facttables",
            "transactionscores",
            "aggregatedtransactionscores",
            "mergetransactionscores",
            "rolluptransactionscores",
            "customerscores",
            "customerrollupscores",
            "aggregatedtransactionsscorecard",
            "mergecustomerscores",
            "entityscores",
            "networkscores",
            "customerscorecard",
            "postprocessscorecard"
          )

          allStages.foreach(stage => {
            logger.info(s"Run stage: $stage")
            runner(stage)
          })
        }
        case "facttables" => SparkExecutor.apply(fiuBatchModelDefinition.factTableScores, fiuPhaseContext)
        case "transactionscores" => SparkExecutor.apply(fiuBatchModelDefinition.transactionDocumentScores, fiuPhaseContext)
        case "mergetransactionscores" => SparkExecutor.apply(fiuBatchModelDefinition.mergeDocumentScores, fiuPhaseContext)
        case "rolluptransactionscores" => SparkExecutor.apply(fiuBatchModelDefinition.rollupFromMergedDocumentScores, fiuPhaseContext)
        case "aggregatedtransactionscores" => SparkExecutor.apply(fiuBatchModelDefinition.aggregatedTransactionDocumentScores, fiuPhaseContext)
        case "customerscores" => SparkExecutor.apply(fiuBatchModelDefinition.customerDocumentScores, fiuPhaseContext)
        case "customerrollupscores" => SparkExecutor.apply(fiuBatchModelDefinition.customerRollupScores, fiuPhaseContext)
        case "mergecustomerscores" => SparkExecutor.apply(fiuBatchModelDefinition.mergeCustomerScores, fiuPhaseContext)
        case "entityscores" => SparkExecutor.apply(fiuBatchModelDefinition.entityScores, fiuPhaseContext)
        case "networkscores" => SparkExecutor.apply(fiuBatchModelDefinition.networkScores, fiuPhaseContext)
        case "customerscorecard" => runCustomerScorecard(projectConfig)(sparkSession)
        case "aggregatedtransactionsscorecard" => runAggregatedTransactionScorecard(projectConfig)(sparkSession)
        case "postprocessscorecard" => postProcessScorecard(projectConfig)(sparkSession)
        case _ =>
          val scoresToRun = fiuPhaseContext.model.scores.filter(_.score.toString.contains(batchScore))
          if (scoresToRun.isEmpty) throw new IllegalArgumentException(s"Unable to find score or phase $batchScore.")
          SparkExecutor.apply(scoresToRun, fiuPhaseContext)
      }
    }
    runner(runBatchScore)
  }

  def runCustomerScorecard(config: ProjectExampleConfig)(spark: SparkSession): Seq[SparkExecutor.Res] = {
    val modelDefinition = CustomerScorecardModelDefinition(config)
    val scorecardScoringContext = SparkScoringContext(spark = spark,
      model = modelDefinition,
      parameterProvider = modelDefinition.parameterProvider,
      modelDefinition.dataDependencyProviderDefinitions)
    SparkExecutor.apply(modelDefinition.scores, scorecardScoringContext)
  }

  def runAggregatedTransactionScorecard(config: ProjectExampleConfig)(spark: SparkSession): Seq[SparkExecutor.Res] = {
    val modelDefinition = AggregatedTransactionsScorecardModelDefinition(config)
    val scorecardScoringContext = SparkScoringContext(spark = spark,
      model = modelDefinition,
      parameterProvider = modelDefinition.parameterProvider,
      modelDefinition.dataDependencyProviderDefinitions)
    SparkExecutor.apply(modelDefinition.scores, scorecardScoringContext)
  }

  def postProcessScorecard(config: ProjectExampleConfig)(spark: SparkSession): Seq[SparkExecutor.Res] = {
    val modelDefinition = CustomerScorecardModelDefinition(config)
    val scorecardScoringContext = SparkScoringContext(spark = spark,
      model = modelDefinition,
      parameterProvider = modelDefinition.parameterProvider,
      modelDefinition.dataDependencyProviderDefinitions)
    SparkExecutor.apply(modelDefinition.postProcessingScores, scorecardScoringContext)
  }

  def runOtherScores(fiuPhaseContext: SparkScoringContext, runBatchScore: String): Unit = {
    val scoresToRun = fiuPhaseContext.model.scores.filter(_.score.toString.contains(runBatchScore))
    if (scoresToRun.isEmpty) throw new IllegalArgumentException(s"Unable to find score or phase $runBatchScore.")
    SparkExecutor.apply(scoresToRun, fiuPhaseContext)
  }
}

