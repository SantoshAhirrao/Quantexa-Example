package com.quantexa.example.etl.projects.fiu.utils

import com.quantexa.etl.address.core.Model.Parsed.ParsedAddress
import com.quantexa.model.core.datasets.ParsedDatasets.{LensParsedAddress, ParsedDateParts}

/*
  * Project parsing utilities/functions shared across input data sources
  */

object ProjectParsingUtils {

  /***
    * Converts a java.sql.Date into [[com.quantexa.model.core.datasets.ParsedDatasets.ParsedDateParts]]
    * @param date The java.sql.Date to parse
    */
  def parseDate(date: Option[java.sql.Date]): Option[ParsedDateParts] = {
    date.map(date => {
      val localDate = date.toLocalDate
      ParsedDateParts(Some(date)
        ,Some("%04d".format(localDate.getYear))
        ,Some("%02d".format(localDate.getMonthValue))
        ,Some("%02d".format(localDate.getDayOfMonth))
      )
    })
  }

  /** Convert a parsed address to LensParsed
    *
    * @param address The ParsedAddress
    * @tparam T The parent type
    * @return The LensParsedAddress
    */
  def parsedAddressToLensParsedAddress[T](address: ParsedAddress) = {
    LensParsedAddress[T](
      addressDisplay = address.addressDisplay,
      flatNumberHouseNumberHouseName = address.flatNumberHouseNumberHouseName,
      houseNumberHouseName = address.houseNumberHouseName,
      flatNumber = address.flatNumber,
      houseName = address.houseName,
      houseNumber = address.houseNumber,
      poBox = address.poBox,
      road = address.road,
      suburb = address.suburb,
      cityDistrict = address.cityDistrict,
      city = address.city,
      state = address.state,
      stateDistrict = address.stateDistrict,
      postcode = address.postcode,
      country = address.country,
      countryCode = address.countryCode)
  }
}
