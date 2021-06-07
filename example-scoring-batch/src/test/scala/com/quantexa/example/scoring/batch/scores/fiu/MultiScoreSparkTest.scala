package com.quantexa.example.scoring.batch.scores.fiu

import java.util.TimeZone

import com.quantexa.example.scoring.batch.model.fiu.{FiuTestDataDependencyProviderDefinitions, FiuTestModelDefinition}
import com.quantexa.example.scoring.batch.scores.fiu.EntityTestData.{documentDS, entityAttributesDF}
import com.quantexa.example.scoring.batch.utils.fiu.SparkTestScoringFrameworkTestSuite
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.parameters.{CSVScoreParameterSource, ScoreParameterProvider}
import com.quantexa.scoring.framework.spark.execution.{EngSparkData, SparkExecutor, SparkScoringContext}
import org.apache.commons.lang3.SystemUtils
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Inside

//TODO: IP-529: Modify this to test the real-time scoring model.
@RunWith(classOf[JUnitRunner])
class MultiScoreSparkTest extends SparkTestScoringFrameworkTestSuite with Inside {

  var ctx: SparkScoringContext = _

  override def beforeAll() = {
    super.beforeAll()
    if (SystemUtils.IS_OS_WINDOWS && Option(System.getenv("HADOOP_HOME")).isEmpty) {
      val errorMsg = """Required environment variable on windows 'HADOOP_HOME' is not set.
                       |Ensure that HADOOP_HOME is set and that
                       |%HADOOP_HOME%/bin/ contains the 'winutils.exe' binary
                       |This is downloadable from 'https://github.com/steveloughran/winutils'
                       |""".stripMargin

      throw new IllegalStateException(errorMsg)
    }

    //This is needed or weird things happen with date times!
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    System.setProperty("engSparkRoot", "file:///" + tmp)

    MultiScoreSparkTest.writeTempData(spark, tmp.toString)

    val csvSource = new CSVScoreParameterSource(filePath = ProjectExampleConfig.default("tmp").scoreParameterFile.filePath, classpath = true)
    val parameterProvider = ScoreParameterProvider.fromSources(Seq(csvSource))

    ctx = SparkScoringContext(
      spark,
      FiuTestModelDefinition,
      parameterProvider,
      Some(new FiuTestDataDependencyProviderDefinitions(ProjectExampleConfig.default(tmp.toString)))
    )
  }

  lazy val results = SparkExecutor.apply(ctx.model.scores, ctx)

  it should "be able to produce results for each type" in {
    assert(results.size == 3) //Six different score types
  }

  it should "create output for the three different entity scores" in {
    assert(results.count(_.folderGrouping == "Entity") == 3)
  }
}

object MultiScoreSparkTest {

  def writeTempData(spark: SparkSession, tempDataPath: String): Unit = {
    val entAttrsTempAbsolutePath = tempDataPath + "/" + EngSparkData.EntityAttributes

    entityAttributesDF(spark).write
      .mode(SaveMode.Overwrite)
      .parquet(entAttrsTempAbsolutePath + "/individual.parquet")

    documentDS(spark).write
      .mode(SaveMode.Overwrite)
      .parquet(tempDataPath + "/DocumentDataModel/CleansedDocumentDataModel.parquet")

  }

}