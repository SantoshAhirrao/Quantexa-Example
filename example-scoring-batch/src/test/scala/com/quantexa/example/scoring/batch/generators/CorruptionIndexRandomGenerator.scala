package com.quantexa.example.scoring.batch.generators

import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper._
import com.quantexa.example.scoring.model.fiu.ScoringModel.CountryCorruption
import org.scalacheck.Gen

import scala.reflect.ClassTag

case object CorruptionIndexRandomGenerator extends GeneratesDataset[CountryCorruption] {

  def generate(implicit ct: ClassTag[CountryCorruption]): Gen[CountryCorruption] = for {
    countriesAndRank <- Gen.oneOf(countriesAndRank)
    iso3 = countriesAndRank._1
    corruptionIndex = countriesAndRank._2.toInt
  } yield CountryCorruption(ISO3 = iso3, corruptionIndex = corruptionIndex)
}
