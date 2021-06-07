package com.quantexa.example.scoring.batch.utils.fiu.excel

import shapeless.tag
import shapeless.tag.@@

object ExcelTableStyles {

  trait TableStyleTag
  type ExcelTableStyle = String @@ TableStyleTag

  //Rows are white
  val tableStyleMedium1: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium1") //Black
  val tableStyleMedium2: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium2") //Dark Blue
  val tableStyleMedium3: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium3") //Orange
  val tableStyleMedium4: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium4") //Grey
  val tableStyleMedium5: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium5") //Yellow
  val tableStyleMedium6: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium6") //Blue
  val tableStyleMedium7: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium7") //Green

  //Rows are filled with colour
  val tableStyleMedium8: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium8") //Black
  val tableStyleMedium9: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium9") //Dark Blue
  val tableStyleMedium10: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium10") //Orange
  val tableStyleMedium11: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium11") //Grey
  val tableStyleMedium12: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium12") //Yellow
  val tableStyleMedium13: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium13") //Blue
  val tableStyleMedium14: ExcelTableStyle = tag[TableStyleTag][String]("TableStyleMedium14") //Green
}
