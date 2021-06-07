package com.quantexa.example.model.fiu.hotlist

/***
  * Raw case class models used to represent the flat input raw CSV files as they are read to parquet
  */

object HotlistRawModel {

  case class HotlistRaw (
                          hotlist_id: Option[Long],
                          date_added: Option[java.sql.Timestamp],
                          name: Option[String],
                          address: Option[String],
                          dob: Option[java.sql.Timestamp],
                          telephone: Option[String]
                        )

}