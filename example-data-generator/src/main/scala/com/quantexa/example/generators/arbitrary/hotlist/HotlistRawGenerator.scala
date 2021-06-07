package com.quantexa.example.generators.arbitrary.hotlist

import java.sql.Date.valueOf

import com.quantexa.example.model.fiu.hotlist.HotlistRawModel.HotlistRaw
import com.quantexa.generators.model._
import com.quantexa.generators.templates.Generator
import com.quantexa.example.generators.config.GeneratorLoaderOptions
import org.scalacheck.Arbitrary

import com.quantexa.generators.utils.GeneratorUtils._

import com.quantexa.generators.utils.GeneratedEntities

class HotlistRawGenerator(generatorConfig: GeneratorLoaderOptions, generatedEntities: GeneratedEntities) extends Generator[HotlistRaw] {

  val dateAddedDateRange = DateRange(valueOf("1992-03-02"), Some(valueOf("2014-10-24")))
  val dobDateRange = DateRange(valueOf("1970-02-28"), Some(valueOf("1990-01-01")))

  def getHotlistRaw : Seq[HotlistRaw] = {
    val arbitraryHotlist = (id: Long) => Arbitrary {
      val seed = new java.util.Random(generatorConfig.seed*id).nextLong
      for {
        hotlist_id <- id.option(1)
        dateAddedRange = genDateRangeBetween(dateAddedDateRange, seed).sample.get
        date_added <- new java.sql.Timestamp(dateAddedRange.from.getTime).option(1)
        name <- genCategory(Seq((0.5,pickRandomFromSequence(generatedEntities.businesses, seed)), (0.5, pickRandomFromSequence(generatedEntities.names, seed).fullName)), seed).option(1)
        address <- pickRandomFromSequence(generatedEntities.addresses, seed).fullAddress.option(0.8, seed)
        dobRange = genDateRangeBetween(dobDateRange, seed).sample.get
        dob <- new java.sql.Timestamp(dobRange.from.getTime).option(0.35, seed)
        telephone: scala.Option[String] <- pickRandomFromSequence(generatedEntities.phoneNumbers, seed).option(0.55, seed)
      } yield HotlistRaw(
        hotlist_id,
        date_added,
        name,
        address,
        dob,
        telephone
      )
    }
    randomWithIds[HotlistRaw](arbitraryHotlist, generatorConfig.numberOfHotlistDocuments, generatorConfig.seed)
  }

  override def generateRecords : Seq[HotlistRaw] = {
    getHotlistRaw
  }
}
