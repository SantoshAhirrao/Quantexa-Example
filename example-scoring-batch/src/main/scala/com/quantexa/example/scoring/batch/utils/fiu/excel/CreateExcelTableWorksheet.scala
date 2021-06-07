package com.quantexa.example.scoring.batch.utils.fiu.excel

import com.quantexa.analytics.test.ImportLiteGraphFromCSV.getClass
import com.quantexa.example.scoring.batch.utils.fiu.excel.ExcelTableStyles.ExcelTableStyle
import org.apache.poi.ss.SpreadsheetVersion
import org.apache.poi.ss.util.{AreaReference, CellReference}
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import org.apache.spark.sql.Dataset
import org.slf4j.LoggerFactory

case class SheetConfiguration[T](sheetName: String, fieldMappings: T => List[FieldMapping], tableStyle: ExcelTableStyle)

case class FieldMapping(fieldName: String, value: Any)

object CreateExcelTableWorksheet {
  def apply[T](ds: Dataset[T],
               configuration: SheetConfiguration[T])(wb: XSSFWorkbook): XSSFWorkbook = {
    import scala.collection.JavaConverters.{getClass => _, _}

    val isOriginallyCached = ds.storageLevel.useMemory || ds.storageLevel.useDisk || ds.storageLevel.useOffHeap

    if (!isOriginallyCached) ds.cache()
    val outputWorkbook = if (ds.count ==0) {
      val logger = LoggerFactory.getLogger(getClass)
      logger.warn("No records found to create excel file")
      wb
    } else {

      val ws = wb.createSheet(configuration.sheetName)

      val table = ws.createTable
      val ctTable = table.getCTTable

      val existingSheetIds = ws.getTables.asScala.toList.map(x => x.getCTTable.getId)
      val sheetId = if (existingSheetIds.isEmpty) 1L else existingSheetIds.max + 1L
      val sheetNameNoSpaces = configuration.sheetName.replaceAll(" ", "")

      ctTable.setName(sheetNameNoSpaces + sheetId)
      ctTable.setDisplayName(sheetNameNoSpaces + sheetId)
      ctTable.setId(sheetId)

      val tableStyle = ctTable.addNewTableStyleInfo()
      tableStyle.setName(configuration.tableStyle)
      tableStyle.setShowColumnStripes(false)
      tableStyle.setShowRowStripes(false)

      def tableHeader(data: T) = configuration.fieldMappings(data)

      val columns = ctTable.addNewTableColumns()
      val sampleOfDs = ds.head
      columns.setCount(tableHeader(sampleOfDs).length)
      val headerRow = getOrCreateRow(ws, 0)

      tableHeader(sampleOfDs).zipWithIndex.foreach {
        case (FieldMapping(headerField, _), idx) =>
          val column = columns.addNewTableColumn()
          column.setName(headerField)
          column.setId(idx + 1)
          headerRow.createCell(idx)
          val cell = headerRow.getCell(idx)
          cell.setCellValue(headerField)
      }

      val data = ds.collect.zipWithIndex

      data.foreach {
        case (rowData, rownum) =>
          val row = getOrCreateRow(ws, rownum + 1)
          val headerAndData = tableHeader(rowData).zipWithIndex
          headerAndData.foreach {
            case (FieldMapping(headerField, headerValue), idx) =>
              row.createCell(idx)
              headerValue match {
                case value: String => row.getCell(idx).setCellValue(value)
                case value: Int => row.getCell(idx).setCellValue(value)
                case value: java.sql.Date => row.getCell(idx).setCellValue(value)
                case value: Double => row.getCell(idx).setCellValue(value)
                case value: Long => row.getCell(idx).setCellValue(value)
                case value: Short => row.getCell(idx).setCellValue(value)
                case value: Boolean => row.getCell(idx).setCellValue(value)
                case value: java.sql.Timestamp => row.getCell(idx).setCellValue(value)
                case _ => throw new IllegalArgumentException(s"Unsupported type for field $headerField")
              }
          }
      }

      val tableArea = new AreaReference(new CellReference(0, 0), new CellReference(data.length, tableHeader(sampleOfDs).length - 1), SpreadsheetVersion.EXCEL2007)

      ctTable.setRef(tableArea.formatAsString)

      tableHeader(sampleOfDs).zipWithIndex.foreach {
        case (_, idx) =>
          idx match {
            case _ => ws.autoSizeColumn(idx)
          }
      }

      wb
    }
    if (!isOriginallyCached) ds.unpersist
    outputWorkbook
  }

  private def getOrCreateRow(ws: XSSFSheet, idx: Int) = {
    if (ws.getRow(idx) == null) ws.createRow(idx); ws.getRow(idx)
  }
}
