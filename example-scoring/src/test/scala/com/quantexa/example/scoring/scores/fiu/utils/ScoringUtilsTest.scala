package com.quantexa.example.scoring.scores.fiu.utils

import com.quantexa.example.scoring.utils.ScoringUtils.sortByValueAndByKey
import org.scalatest.{FlatSpec, Matchers}

class ScoringUtilsTest extends FlatSpec with Matchers{

  "Custom sort function" should "sort the tuples on the value in desc order, if the values are different, otherwise on the keys in alphabetical order" in {
    val seqToTest = Seq(("Oxford", 1), ("Manchester", 2), ("London", 3), ("Bristol", 2))

    val expected = Seq(("London", 3), ("Bristol", 2), ("Manchester", 2), ("Oxford", 1))

    val result = seqToTest
      .sortWith( sortByValueAndByKey )

    result shouldBe expected
  }

}
