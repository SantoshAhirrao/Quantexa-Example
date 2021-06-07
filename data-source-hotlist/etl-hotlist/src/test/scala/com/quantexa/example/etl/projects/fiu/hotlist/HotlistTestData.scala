package com.quantexa.example.etl.projects.fiu.hotlist

import com.quantexa.example.etl.projects.fiu.utils.SparkTestSession.spark.sqlContext.implicits._
import com.quantexa.example.model.fiu.hotlist.HotlistRawModel.HotlistRaw

object HotlistTestData {

  val hotlistData = List(
    HotlistRaw(Some(884763262),Some(java.sql.Timestamp.valueOf("1995-10-14 00:00:00")),Some("J Patel"),Some("40 Essex Rd TANTOBIE DH9 1TZ"),Some(java.sql.Timestamp.valueOf("1980-11-15 00:00:00")),None),
    HotlistRaw(Some(893353197),Some(java.sql.Timestamp.valueOf("2006-03-14 00:00:00")),Some("Owen Purcell"),Some("2394 Leslie Street Newmarket ON L3Y 2A3"),Some(java.sql.Timestamp.valueOf("1980-11-22 00:00:00")),None),
    HotlistRaw(Some(927712935),Some(java.sql.Timestamp.valueOf("2005-07-03 00:00:00")),Some("Cody Postle"),Some("3 Frouds Road NUNGURNER VIC 3909"),Some(java.sql.Timestamp.valueOf("1982-10-27 00:00:00")),None),
    HotlistRaw(Some(936302870),Some(java.sql.Timestamp.valueOf("2010-07-16 00:00:00")),Some("Anh Borst"),Some("Dopheistraat 105 Reuver LI 5953 MH"),Some(java.sql.Timestamp.valueOf("1978-01-19 00:00:00")),None),
    HotlistRaw(Some(970662608),Some(java.sql.Timestamp.valueOf("2007-01-11 00:00:00")),Some("Michael Hill"),Some("2452 O Conner Street Pascagoula MS 39567"),Some(java.sql.Timestamp.valueOf("1986-04-25 00:00:00")),None),
    HotlistRaw(Some(1005022347),Some(java.sql.Timestamp.valueOf("2011-09-16 00:00:00")),Some("A Nava"),Some("3847 Cherry Tree Drive Jacksonville FL 32202"),Some(java.sql.Timestamp.valueOf("1972-01-09 00:00:00")),None),
    HotlistRaw(Some(1108101562),Some(java.sql.Timestamp.valueOf("2006-01-25 00:00:00")),Some("Forest City Imports EEI"),Some("73 Monteagle Road HOLT ACT 2615"),None,Some("(02) 6112 8592")),
    HotlistRaw(Some(1116691496),Some(java.sql.Timestamp.valueOf("1997-02-12 00:00:00")),Some("Plan Smart Imports R.L"),Some("17 Redesdale Rd EPSOM VIC 3551"),None,Some("(03) 5341 2940")),
    HotlistRaw(Some(1159641169),Some(java.sql.Timestamp.valueOf("2014-10-24 00:00:00")),Some("H Thomas"),Some("2274 Isabella Street Pembroke ON K8A 5S5"),Some(java.sql.Timestamp.valueOf("1984-04-13 00:00:00")),None)
  ).toDS.as[HotlistRaw]

}
