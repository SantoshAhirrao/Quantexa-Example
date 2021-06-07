package com.quantexa.example.generators.config

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}
import io.circe.config.syntax.CirceConfigOps
import io.circe.generic.auto._

class GeneratorLoaderOptionsTests  extends FlatSpec with Matchers {

  "GeneratorLoaderOption" should "contain a sequence of 2 genders in the test config" in {
    import GeneratorLoaderImplicits._
    val generatorLoaderOptions: GeneratorLoaderOptions =
      ConfigFactory.load("test-config.conf").getConfig("config").as[GeneratorConfig].right.get
    val genders = generatorLoaderOptions.possibleGenders
    genders.size shouldEqual 2
  }

}
