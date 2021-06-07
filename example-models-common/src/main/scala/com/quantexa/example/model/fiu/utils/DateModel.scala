package com.quantexa.example.model.fiu.utils

import com.quantexa.resolver.ingest.BrokenLenses.BOpticOps.traversal

object DateModel {

  private val NumberOfSecondsInDay = 86400

  val zoneIdDefault = java.time.ZoneId.systemDefault()

  val defaultDates = Set(
    java.sql.Date.valueOf("1900-01-01"),
    java.sql.Date.valueOf("9999-12-31")
  )

  val maxDate = java.sql.Date.valueOf("2020-12-31")

  val defaultDateFilter = traversal[java.sql.Date, java.sql.Date] { date =>
    if (!DateModel.defaultDates(date) && date.getTime < maxDate.getTime) Some(date)
    else None
  }

  def generateDateRange(min: java.sql.Date, max: java.sql.Date): Seq[java.sql.Date] = {
    (min.toLocalDate.atStartOfDay(zoneIdDefault).toEpochSecond to max.toLocalDate.atStartOfDay(zoneIdDefault).toEpochSecond
      by NumberOfSecondsInDay).toList
    .map(secondsSinceEpoch => new java.sql.Date(secondsSinceEpoch*1000))
  }
}