package com.quantexa.example.scoring.batch.utils.fiu

import com.quantexa.example.scoring.batch.utils.fiu.RunScoresInSparkSubmit.readParameters
import com.quantexa.example.scoring.utils.TypedConfigReader.ScoreParameterFile
import com.quantexa.scoring.framework.parameters.{ParameterIdentifier, StringScoreParameter}
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class CsvParameterProviderTest extends FlatSpec  {

  "readParameters" should "load parameterProvider map containing expectedIdentifier and expectedParameter" in {

    val expectedIdentifier = ParameterIdentifier(None,"HighRisk_DescriptionString")
    val expectedParameter = StringScoreParameter("HighRisk_DescriptionString","Text for 'high' risk level, to be used alongside high risk banding in score descriptions","high")
    val parameterFileName = "/parameterFile.csv"
    val parameterFilePath = getClass.getResource(parameterFileName).getPath
    val parameterProvider = readParameters(ScoreParameterFile(parameterFilePath))
    val collectedParameter = parameterProvider.parameters(expectedIdentifier)

    assert(collectedParameter === expectedParameter)
  }

}
