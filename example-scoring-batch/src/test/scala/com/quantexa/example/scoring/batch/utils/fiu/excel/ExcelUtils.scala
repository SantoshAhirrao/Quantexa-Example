package com.quantexa.example.scoring.batch.utils.fiu.excel

import java.sql.Date
import java.text.SimpleDateFormat

import org.apache.commons.lang3.StringUtils
import org.apache.poi.ss.usermodel.{Cell, CellType, Row}

object ExcelUtils {

  def filterByDate (row: Row, dateRangesToFilter: Map[java.sql.Date, java.sql.Date], datePos: Int)(implicit formatter: SimpleDateFormat): Boolean = {
    val date = row.getCell(datePos).asDate

    dateRangesToFilter.exists {
      case (startDate, endDate) =>
        date.equals(startDate) || date.equals(endDate) || (date.after(startDate) && date.before(endDate))
    }
  }

  def rowsSorted(leftRow: Row, rightRow: Row, datePos: Int): Boolean =
    leftRow.getCell(datePos).asDate.before(rightRow.getCell(datePos).asDate)


  def filterRowsBySpecificNotEmptyCell(row: Row, cellPos: Int): Boolean = {
      val cell = row.getCell(cellPos)

      cell != null && cell.getCellTypeEnum != CellType.BLANK && StringUtils.isNoneBlank(cell.toString)
  }

  def dateRangesStringToDateFormat(dateRangesToFilter: Map[String, String]): Map[Date, Date] = dateRangesToFilter.map {
    case (startDate, endDate) => (java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate))
  }

  implicit def toSafeCell(cell: Cell): SafeCell = SafeCell(cell)
  implicit val formatter: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
}

final case class ExcelParserException(private val message: String = "", private val cause: Throwable = None.orNull) extends Exception(message, cause)
