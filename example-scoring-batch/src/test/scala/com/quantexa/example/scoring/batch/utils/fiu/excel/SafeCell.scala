package com.quantexa.example.scoring.batch.utils.fiu.excel

import java.text.SimpleDateFormat
import java.sql.Date

import org.apache.poi.ss.usermodel.Cell

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

case class SafeCell(cell: Cell){
  val reference = s"${cell.getSheet.getSheetName}! ${cell.getAddress}"

  def asOptionDouble: Option[Double] = Try(cell.getNumericCellValue) match {
    case Success(r) => Some(r)
    case Failure(e) => asString match {
      case "None" => None
      case "N/A" => Some(Double.NaN)
      case _ => throw ExcelParserException (reference, e)
    }
  }

  def asDouble: Double = Try(cell.getNumericCellValue) match {
    case Success(r) => r
    case Failure(e) => throw ExcelParserException(reference, e)
  }

  def asLong: Long = doubleToLong(asDouble).getOrElse(throw ExcelParserException(reference))

  def asString: String = Try(cell.getStringCellValue) match {
    case  Success(r) => r
    case Failure(e) => throw ExcelParserException(reference, e)
  }

  def asOptionString: Option[String] = Try(cell.getStringCellValue) match {
    case  Success(r) => r match {
      case "unknown" => None
      case _ => Some(r)
    }
    case Failure(e) => throw ExcelParserException(reference, e)
  }

  def asDateString(implicit formatter: SimpleDateFormat): String = Try(cell.getDateCellValue) match {
    case  Success(r) => formatter.format(r)
    case Failure(e) => throw ExcelParserException(reference, e)
  }

  def asDate(implicit formatter: SimpleDateFormat): Date = java.sql.Date.valueOf(asDateString)

  def asMap: Map[String, Long] = {
    def clean(s: String) = {
      s.replace("Map(", "").replace(")", "").replace("\"", "").trim
    }

    val cleanString = clean(asString)
    cleanString.length match {
      case 0 => Map[String, Long]()
      case _ => ListMap( cleanString.split(",").map(_.split("->"))
        .map(keyValue => (keyValue(0).trim, keyValue(1).trim.toLong)).toList : _*)
    }
  }

  private def doubleToLong(d: Double): Option[Long] =
    if(d.isValidInt) Some(d.toLong) else None
}

