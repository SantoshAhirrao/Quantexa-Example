package com.quantexa.example.scoring.batch.utils.fiu

import java.nio.file.Paths

class EndToEndScoringTest extends ScoringGeneratedDataTestSuite {

  override def initialiseScoreOutputRoot(other: String = "scoring"): Unit = {
    val scoringTmp = Paths.get(tmp.toString, other).toString
    System.setProperty("scoreOutputRoot", scoringTmp)
    System.setProperty("scoreMode","test")
  }

  it should "test for runtime errors in the scoring pipeline" in {
    // when creating a new score add it here to be tested

    val stagesInOrder = List(
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

    stagesInOrder.foreach {
      stage =>
        logger.info(s"Testing Stage $stage")
        System.setProperty("runBatchScore", stage)
        RunScoresInSparkSubmit.run(spark, logger, args = Seq(), testConfig, metrics)
    }
  }
}