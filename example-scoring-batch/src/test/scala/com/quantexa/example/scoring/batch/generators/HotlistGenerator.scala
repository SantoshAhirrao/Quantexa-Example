package com.quantexa.example.scoring.batch.generators

import java.util.Calendar

import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist
import com.quantexa.example.scoring.scores.fiu.generators.GeneratorHelper._
import com.quantexa.model.core.datasets.ParsedDatasets._
import org.scalacheck.Gen

import scala.reflect.ClassTag


case object HotlistGenerator extends GeneratesDataset[Hotlist] {


  val lensParsedBusinessGenerator: Gen[LensParsedBusiness[Hotlist]] = for {
    businessDisplay <- Gen.option(businessNameGen)
    businessName = businessDisplay
    businessNameClean <- Gen.option(Gen.containerOfN[Seq, String](5, businessNameGen))

  } yield LensParsedBusiness(
    businessDisplay,
    businessName,
    businessNameClean
  )


  val parsedDatePartsGenerator: Gen[ParsedDateParts] = for {
    date <- Gen.option(javaSqlDateGenerator(defaultStartDate, defaultDateRange))
    calendar = date.map {
      d =>
        val cal = Calendar.getInstance()
        cal.setTime(d)
        cal
    }
    year = calendar.map(_.get(Calendar.YEAR).toString)
    month = calendar.map(_.get(Calendar.MONTH).toString)
    day = calendar.map(_.get(Calendar.DAY_OF_MONTH).toString)
  } yield ParsedDateParts(date, year, month, day)


  val lensParsedIndividualNameGenerator: Gen[LensParsedIndividualName[Hotlist]] = for {
    forename <- Gen.option(forenameGen)
    middlename <- Gen.option(forenameGen)
    surname <- Gen.option(surnameGen)
    names = Seq(forename, middlename, surname).flatten
    initial <- Gen.option(Gen.const(names.map(_.head).mkString(".")))
    nameDisplay <- Gen.option(Gen.const(names.mkString(" ")))
    nodeId = (nameDisplay, forename, middlename, surname, initial).hashCode()
  } yield LensParsedIndividualName(
    nodeId,
    nameDisplay,
    forename,
    middlename,
    surname,
    initial
  )
  val lensParsedAddressGenerator: Gen[LensParsedAddress[Hotlist]] = for {

    houseNumberHouseName <- Gen.option(Gen.alphaStr)
    flatNumber <- Gen.option(Gen.numStr)
    houseName <- Gen.option(surnameGen.map(_ +" house"))
    houseNumber <- Gen.option(Gen.numStr)
    poBox <- Gen.option(Gen.alphaStr.map("PO Box. " + _))
    flatNumberHouseNumberHouseName = Option(Seq(flatNumber, houseNumber, houseName, poBox).flatten.mkString(" ")).filter(!_.isEmpty)
    road <- Gen.option(Gen.alphaStr)
    suburb <- Gen.option(Gen.alphaStr)
    cityDistrict <- Gen.option(Gen.alphaStr)
    city <- Gen.option(Gen.alphaStr)
    state <- Gen.option(Gen.alphaStr)
    stateDistrict <- Gen.option(Gen.alphaStr)
    postcode <- Gen.option(Gen.identifier)
    country <- Gen.option(countryGen)
    countryCode <- Gen.option(countryIsoGen)
    addressDisplay = s" \nLine1: ${flatNumberHouseNumberHouseName.getOrElse("")} \n Line2: ${country.getOrElse("")}"
  } yield LensParsedAddress(
    addressDisplay,
    flatNumberHouseNumberHouseName,
    houseNumberHouseName,
    flatNumber,
    houseName,
    houseNumber,
    poBox,
    road,
    suburb,
    cityDistrict,
    city,
    state,
    stateDistrict,
    postcode,
    country,
    countryCode
  )
  val lensParsedTelephoneGenerator: Gen[LensParsedTelephone[Hotlist]] = for {
    telephoneDisplay <- Gen.alphaStr
    telephoneClean <- Gen.alphaStr
    countryCode <- Gen.option(Gen.oneOf(countriesIso))
  } yield LensParsedTelephone(
    telephoneDisplay,
    telephoneClean,
    countryCode
  )

   def generate(implicit ct: ClassTag[Hotlist]): Gen[Hotlist] = {
    val generator: Gen[Hotlist] = for {


      dateAdded <- Gen.option(javaSqlDateGenerator(defaultStartDate, defaultDateRange))
      registeredBusinessName <- Gen.option(businessNameGen)
      parsedBusinessName <- Gen.option(lensParsedBusinessGenerator)
      personalBusinessFlag <- Gen.option(yesNoGen)
      dateOfBirth <- Gen.option(javaSqlDateGenerator(defaultStartDate, defaultDateRange))
      dateOfBirthParts <- Gen.option(parsedDatePartsGenerator)
      fullName <- Gen.option(fullnameGen)
      parsedIndividualName <- Gen.containerOfN[List, LensParsedIndividualName[Hotlist]](10,lensParsedIndividualNameGenerator).map(_.toSeq)
      consolidatedAddress <- Gen.option(Gen.alphaStr)
      parsedAddress <- Gen.option(lensParsedAddressGenerator)
      telephoneNumber <- Gen.option(Gen.numStr)
      cleansedTelephoneNumber <- Gen.option(lensParsedTelephoneGenerator)
      hotlistId = (
        dateAdded,
        registeredBusinessName,
        parsedBusinessName,
        personalBusinessFlag,
        dateOfBirth,
        dateOfBirthParts,
        fullName,
        parsedIndividualName,
        consolidatedAddress,
        parsedAddress,
        telephoneNumber,
        cleansedTelephoneNumber
      ).hashCode
    } yield
      Hotlist(
        hotlistId,
        dateAdded,
        registeredBusinessName,
        parsedBusinessName,
        personalBusinessFlag,
        dateOfBirth,
        dateOfBirthParts,
        fullName,
        parsedIndividualName,
        consolidatedAddress,
        parsedAddress,
        telephoneNumber,
        cleansedTelephoneNumber
      )
    generator
  }
}
