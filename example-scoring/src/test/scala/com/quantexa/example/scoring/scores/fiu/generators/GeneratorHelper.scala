package com.quantexa.example.scoring.scores.fiu.generators

import java.io.InputStream
import java.sql.{Date, Timestamp}
import java.time.Instant

import org.joda.time.{DateTime, Period}
import org.scalacheck.Gen

import scala.io.{BufferedSource, Source}
import scala.util.Try

object GeneratorHelper {
  //utility
  val maybeStringGen = Gen.option(Gen.alphaStr)
  val maybeBooleanGen = Gen.option(Gen.oneOf(true, false))
  val yesNoGen = Gen.oneOf("Y", "N")
  val someStringGen = Gen.some(Gen.alphaStr)
  val sexes = Seq("M","F", "G/N")
  val genderGen = Gen.oneOf(sexes)
  val idGen = Gen.identifier

  //names
  val forenames = List("Bob", "Rob", "Monica", "Lili", "Iris", "Emily", "Emma")
  val surnames = List("Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller")
  val forenameGen = Gen.oneOf(forenames)
  val surnameGen = Gen.oneOf(surnames)
  val fullnameGen = for {
    fn <- forenameGen
    sn <- surnameGen
  } yield (s"$fn , $sn")
  val businessNameGen = Gen.pick(3, GeneratorHelper.surnames).map((names :Seq[String]) => names.init.mkString(", ") + s" & ${names.last} Ltd")

  //auto closing utility
  def using[A <: {def close() : Unit}, B](param: A)(f: A => B): B =
    try f(param) finally param.close()

  //countries
  val corruptCountriesPath = "/scoring/countries/corruptcountriesindex.csv"
  lazy val countryData: List[Array[String]] = {
    val result = using(getClass.getResourceAsStream(corruptCountriesPath)) {
      is: InputStream =>
        using(Source.fromInputStream(is)) {
          content: BufferedSource =>
            val data = content.getLines.toSeq
            val headerlessData = data.tail.toList
            headerlessData.map { line =>
              line.split(",").map(_.trim)
            }
        }
    }
    result
  }
  val isoColumnIndex = 1
  val countryColumnIndex = 0
  val countryRankColumnIndex = 4

  lazy val countriesAndRank = countryData.map(
    (cd: Array[String]) =>
      (cd(countryColumnIndex), cd(countryRankColumnIndex)))
  lazy val countries = countryData.map((cd: Array[String]) => cd(countryColumnIndex))
  lazy val countriesIso = countryData.map((cd: Array[String]) => cd(isoColumnIndex))
  lazy val countryGen=Gen.oneOf(countries)
  lazy val countryIsoGen=Gen.oneOf(countriesIso)
  lazy val ibanGen =  for {
    iso <- countryIsoGen
    bban   <- Gen.listOfN(30, Gen.alphaChar).map(_.mkString(""))
    check  <- Gen.listOfN(2, Gen.numChar).map(_.mkString(""))
  } yield iso + check + bban

  // currencies products
  val productTypes = Seq("eur", "usd", "pound")
  val currencies = Seq("eur", "usd", "pound")

  // date utilities
  val defaultStartDate = new DateTime(2016, 1, 1, 0, 0)
  val defaultDateRange = Period.years(1)

  def dateGen(lowerInstant: Option[Date]) = Gen.chooseNum(lowerInstant.getOrElse(java.util.Date.from(Instant.EPOCH)).getTime, Instant.now().toEpochMilli).map(new Date(_))

  def convertToSqlDate(day: Int, month: Int, year: Int): Option[java.sql.Date] =
    Try {
      val t = java.util.Calendar.getInstance
      t.set(year, month, day, 12, 11, 10)
      new java.sql.Date(1000 * (t.getTimeInMillis / 1000)) //This operation to avoid getting milliseconds
    }.toOption

  def convertToSqlTimestamp(day: Int, month: Int, year: Int, hourOfDay: Int, minute: Int, second: Int): Option[java.sql.Timestamp] =
    Try {
      val t = java.util.Calendar.getInstance
      t.set(year, month, day, hourOfDay, minute, second)
      new Timestamp(1000 * (t.getTimeInMillis / 1000)) //This operation to avoid getting milliseconds
    }.toOption

  def javaSqlDateGenerator(from: DateTime, range: Period): Gen[java.sql.Date] = {
    val start = from.getMillis
    val end = from.plus(range).getMillis
    for {
      time: Long <- Gen.chooseNum(start, end)
    } yield new java.sql.Date(time)
  }
}
