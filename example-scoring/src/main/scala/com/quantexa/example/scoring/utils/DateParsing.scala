package com.quantexa.example.scoring.utils

import java.text.SimpleDateFormat
import scala.util.Try

object DateParsing {
  val slashFormatTime = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss")
  slashFormatTime.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))
  val dashFormatTime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
  dashFormatTime.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))
  val slashFormat = new SimpleDateFormat("yyyy/MM/dd")
  slashFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))
  val dashFormat = new SimpleDateFormat("yyyy-MM-dd")
  dashFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))
  val justYearMonthSlashFormat = new SimpleDateFormat("yyyy/MM")
  justYearMonthSlashFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))
  val justYearMonthDashFormat = new SimpleDateFormat("yyyy-MM")
  justYearMonthDashFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))
  val justYearFormat = new SimpleDateFormat("yyyy")
  justYearFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))
  val noSeperatorFormat = new SimpleDateFormat("YYYYMMDD")
  noSeperatorFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))

  val mantasDateFormats = Seq(
    slashFormatTime,
    dashFormatTime,
    slashFormat,
    dashFormat,
    justYearMonthSlashFormat,
    justYearMonthDashFormat,
    justYearFormat
  )

  def parseDate(d: String, fmts: Seq[SimpleDateFormat] = mantasDateFormats): java.sql.Date = {
    tryParseDate(d, fmts).getOrElse(throw new IllegalArgumentException(s"Could not parse date from string: $d"))
  }

  def parseDateOrDefault(d: String, fmts: Seq[SimpleDateFormat] = mantasDateFormats): java.sql.Date = {
    tryParseDate(d, fmts).getOrElse(tryParseDate("1960-01-01", fmts).get)
  }

  def tryParseDate(d: String, fmts: Seq[SimpleDateFormat]): Option[java.sql.Date] = {
    val startingDate: Option[java.sql.Date] = None
    fmts.foldLeft(startingDate) {
      case (dateOpt, fmt) =>
        if (dateOpt.isDefined) dateOpt
        else Try(fmt.parse(d)).toOption.map(d => new java.sql.Date(d.getTime))
    }
  }
}