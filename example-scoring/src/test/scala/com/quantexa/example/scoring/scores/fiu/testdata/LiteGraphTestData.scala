package com.quantexa.example.scoring.scores.fiu.testdata

import com.quantexa.analytics.test.ImportLiteGraphFromCSV.getLiteGraphWithId
import com.quantexa.resolver.core.EntityGraphLite.LiteGraphWithId

object LiteGraphTestData {

  val inputPaths = Seq(
    getClass.getResource("/scoring/litegraphs/General/simpleCustomerNetwork").getPath,
    getClass.getResource("/scoring/litegraphs/HighNumberOfHotlistDocsOnNetwork/highNumberOfHotlists").getPath,
    getClass.getResource("/scoring/litegraphs/HighNumberOfHotlistDocsOnNetwork/smallNumberOfHotlists").getPath,
    getClass.getResource("/scoring/litegraphs/HighNumberOfMaleAUIndividuals/highNumberOfAUMaleIndividuals").getPath,
    getClass.getResource("/scoring/litegraphs/HighNumberOfMaleAUIndividuals/smallNumberOfAUMaleIndividuals").getPath,
    getClass.getResource("/scoring/litegraphs/HighNumberOfMaleUSIndividualsConnectedToHotlist/highNumberOfUSMaleIndividuals").getPath,
    getClass.getResource("/scoring/litegraphs/HighNumberOfMaleUSIndividualsConnectedToHotlist/smallNumberOfUSMaleIndividuals").getPath,
    getClass.getResource("/scoring/litegraphs/HighNumberRecentlyJoinedIndividualCustomersOnNetwork/highNumberOfRecentlyJoinedIndividualCustomers").getPath,
    getClass.getResource("/scoring/litegraphs/HighNumberRecentlyJoinedIndividualCustomersOnNetwork/smallNumberOfRecentlyJoinedIndividualCustomers").getPath
  )

    def getOrElse[B1 >: LiteGraphWithId](key: String, default: => B1 = throw new NoSuchElementException(s"Test case not found in src/test/resources")): B1  = inputPaths.map{ str =>
      val filename = str.split("/").last
      (filename,getLiteGraphWithId(str))
    }.toMap.getOrElse(key, default)

}