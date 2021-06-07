package com.quantexa.example.etl.projects.fiu.customer

import com.quantexa.example.etl.projects.fiu.utils.SparkTestSession.spark.sqlContext.implicits._
import com.quantexa.example.model.fiu.customer.CustomerRawModel.{CustomerRaw,AccountRaw,AccToCusRaw}

object CustomerTestData {

  val customerData = List(
    CustomerRaw(Some(130585981),Some("C"),Some("Leo's Stereo S.M.B.A"),Some("7771371"),Some("AU"),Some(java.sql.Timestamp.valueOf("1990-12-05 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-11-14 00:00:00")),Some("B"),None,None,None,None,None,None,None,Some("."),None,Some("AU"),Some("85 Mendooran Road ANGLE PARK NSW2830."),Some("."),None),
    CustomerRaw(Some(830025268),Some("A"),Some("Newhair EE"),Some("582347"),Some("NL"),Some(java.sql.Timestamp.valueOf("1998-11-21 00:00:00")),None,Some("B"),None,None,None,None,None,None,None,Some("."),None,Some("NL"),Some("Europa-Ring 50 Wierden OV 7641 DG"),Some("."),None),
    CustomerRaw(Some(244237377),Some("A"),None,Some("."),None,Some(java.sql.Timestamp.valueOf("2006-04-21 00:00:00")),None,Some("P"),Some("M"),Some("A"),Some("Albert"),Some("Ramsey"),None,Some("Albert Ramsey"),Some("ZA"),Some("5947"),Some("f"),Some("ZA"),Some("238 Bo Meul St Retreat Western Cape 7945"),Some("454302805"),Some(java.sql.Timestamp.valueOf("1973-01-27 00:00:00"))),
    CustomerRaw(Some(986113362),Some("A"),None,Some("."),None,Some(java.sql.Timestamp.valueOf("2009-10-10 00:00:00")),None,Some("P"),Some("M"),Some("A"),Some("Aidan"),Some("Semmens"),None,Some("Aidan Semmens"),Some("AU"),Some("3371"),Some("f"),Some("AU"),Some("22 Jacabina Court COLEDALE NSW 2515"),Some("98071930"),Some(java.sql.Timestamp.valueOf("1979-07-07 00:00:00"))),
    CustomerRaw(Some(535525763),Some("C"),None,Some("."),None,Some(java.sql.Timestamp.valueOf("1999-09-07 00:00:00")),Some(java.sql.Timestamp.valueOf("2006-07-15 00:00:00")),Some("P"),Some("F"),Some("S"),Some("Samantha"),Some("Mary"),None,Some("Samantha Marx"),Some("US"),Some("3822"),Some("f"),Some("US"),Some("4614 Radford Street Winchester KY 40391"),Some("544090508"),Some(java.sql.Timestamp.valueOf("1973-04-01 00:00:00"))),
    CustomerRaw(Some(633910089),Some("C"),Some("Schaak Electronics SICAV"),Some("35497"),Some("AU"),Some(java.sql.Timestamp.valueOf("1991-12-03 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-08-18 00:00:00")),Some("B"),None,None,None,None,None,None,None,Some("."),None,Some("AU"),Some("69 Boland Drive COOPERS SHOO NSW 2479"),Some("."),None)
  ).toDS.as[CustomerRaw]

  val accountToCustomerData = List(
    AccToCusRaw(Some(130585981),Some(247351693),Some(java.sql.Timestamp.valueOf("1990-12-05 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-11-14 00:00:00")),Some(java.sql.Timestamp.valueOf("1990-12-05 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-11-14 00:00:00")),Some("CUST ACCT LINK"),Some("AU"),Some(java.sql.Timestamp.valueOf("1990-12-05 00:00:00"))),
    AccToCusRaw(Some(830025268),Some(276031267),Some(java.sql.Timestamp.valueOf("1998-11-21 00:00:00")),None,Some(java.sql.Timestamp.valueOf("1998-11-21 00:00:00")),None,Some("CUST ACCT LINK"),Some("NL"),Some(java.sql.Timestamp.valueOf("1998-11-21 00:00:00"))),
    AccToCusRaw(Some(244237377),Some(395737332),Some(java.sql.Timestamp.valueOf("2006-04-21 00:00:00")),None,Some(java.sql.Timestamp.valueOf("2006-04-21 00:00:00")),None,Some("CUST ACCT LINK"),Some("ZA"),Some(java.sql.Timestamp.valueOf("2006-04-21 00:00:00"))),
    AccToCusRaw(Some(986113362),Some(411562178),Some(java.sql.Timestamp.valueOf("2009-10-10 00:00:00")),None,Some(java.sql.Timestamp.valueOf("2009-10-10 00:00:00")),None,Some("CUST ACCT LINK"),Some("AU"),Some(java.sql.Timestamp.valueOf("2009-10-10 00:00:00"))),
    AccToCusRaw(Some(535525763),Some(461420150),Some(java.sql.Timestamp.valueOf("1999-09-07 00:00:00")),Some(java.sql.Timestamp.valueOf("2006-07-15 00:00:00")),Some(java.sql.Timestamp.valueOf("1999-09-07 00:00:00")),Some(java.sql.Timestamp.valueOf("2006-07-15 00:00:00")),Some("CUST ACCT LINK"),Some("US"),Some(java.sql.Timestamp.valueOf("1999-09-07 00:00:00"))),
    AccToCusRaw(Some(633910089),Some(467574091),Some(java.sql.Timestamp.valueOf("1991-12-03 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-08-18 00:00:00")),Some(java.sql.Timestamp.valueOf("1991-12-03 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-08-18 00:00:00")),Some("CUST ACCT LINK"),Some("AU"),Some(java.sql.Timestamp.valueOf("1991-12-03 00:00:00")))
  ).toDS.as[AccToCusRaw]

  val accountData = List(
    AccountRaw(Some(247351693),Some("Indiewealth Agricultural"),Some("65 Nerrigundah Drive BLIND BIGHT VIC 3980"),Some("Active"),Some("BLIND BIGHT"),None,Some("Indiewealth Agricultural"),None,None,Some("65 Nerrigundah Drive"),Some("BLIND BIGHT"),Some("VIC"),Some("3980"),Some("AU"),Some(2952),Some("f"),None,Some("(03) 9695 8803")),
    AccountRaw(Some(276031267),Some("Littler's Ek."),Some("88 Berambing Crescent LAPSTONE NSW 2773"),Some("Active"),Some("LAPSTONE"),None,Some("Littler's Ek."),None,None,Some("88 Berambing Crescent"),Some("LAPSTONE"),Some("NSW"),Some("2773"),Some("AU"),Some(6775),Some("f"),None,Some("(02) 4783 9956")),
    AccountRaw(Some(395737332),Some("Stratapro Imports PLC"),Some("2399 Mulberry Lane West Palm Be FL 33401"),Some("Active"),Some("West Palm Be"),None,Some("Stratapro Imports PLC"),None,None,Some("2399 Mulberry Lane"),Some("West Palm Be"),Some("FL"),Some("33401"),Some("US"),Some(8873),Some("f"),None,Some("561-804-7959")),
    AccountRaw(Some(411562178),Some("JUSTIN BIRCHELL"),Some("9 Quayside Vista WATSON ACT 2602"),Some("Active"),Some("WATSON"),Some("ZACHARIAH VLASAK"),Some("JUSTIN BIRCHELL"),None,None,Some("9 Quayside Vista"),Some("WATSON"),Some("ACT"),Some("2602"),Some("AU"),Some(8191),Some("f"),None,Some("(02) 6173 4172")),
    AccountRaw(Some(461420150),Some("Netcore Imports VOF"),Some("Piazzetta Scalette Rubiani 114 Salento SA 84070"),Some("Active"),Some("Salento"),None,Some("Netcore Imports VOF"),None,None,Some("Via Goffredo Mameli 139"),Some("Salento"),Some("RI"),Some("2030"),Some("IT"),Some(7689),Some("f"),None,Some("0357 6904115")),
    AccountRaw(Some(467574091),Some("OROSCO MATA"),Some("R Combatentes G Guerra 98 Casal da Rob Coimbra 3080-399"),Some("Active"),Some("Casal da Rob"),None,Some("OROSCO MATA"),None,None,Some("R Combatentes G Guerra 98"),Some("Casal da Rob"),Some("Coimbra"),Some("3080-399"),Some("PT"),Some(9349),Some("f"),None,Some("21 239 454 4855"))
  ).toDS.as[AccountRaw]

  val customerUpdateData = List(
    CustomerRaw(Some(905790248),Some("C"),Some("Practi-Plan Mapping Imports Corporation"),Some("53878107"),Some("MU"),Some(java.sql.Timestamp.valueOf("1991-05-11 00:00:00")),Some(java.sql.Timestamp.valueOf("1995-03-02 00:00:00")),Some("B"),None,None,None,None,None,None,None,Some("."),None,Some("AU"),Some("49 Creedon Street BRUNSWICK NO VIC 3056"),Some("."),None),
    CustomerRaw(Some(187851146),Some("C"),None,Some("."),None,Some(java.sql.Timestamp.valueOf("1992-08-12 00:00:00")),Some(java.sql.Timestamp.valueOf("2001-08-23 00:00:00")),Some("P"),Some("F"),Some("A"),Some("Ashley"),Some("Smith"),None,Some("Ashley Smith"),Some("US"),Some("5961"),Some("f"),Some("US"),Some("2440 Rardin Drive San Mateo CA 94403"),Some("195527997"),Some(java.sql.Timestamp.valueOf("1973-07-18 00:00:00"))),
    CustomerRaw(Some(958151233),Some("C"),None,Some("."),None,Some(java.sql.Timestamp.valueOf("1996-03-11 00:00:00")),Some(java.sql.Timestamp.valueOf("2009-04-09 00:00:00")),Some("P"),Some("M"),Some("S"),Some("Sebastia"),Some("Becker"),None,Some("Sebastia Becker"),Some("CH"),Some("5080"),Some("f"),Some("CH"),Some("Im Wingert 70 Leibstadt 5325"),Some("344511459"),Some(java.sql.Timestamp.valueOf("1986-06-26 00:00:00")))
  ).toDS.as[CustomerRaw]

  val accountToCustomerUpdateData = List(
    AccToCusRaw(Some(905790248),Some(519209733),Some(java.sql.Timestamp.valueOf("1991-05-11 00:00:00")),Some(java.sql.Timestamp.valueOf("1995-03-02 00:00:00")),Some(java.sql.Timestamp.valueOf("1991-05-11 00:00:00")),Some(java.sql.Timestamp.valueOf("1995-03-02 00:00:00")),Some("CUST ACCT LINK"),Some("AU"),Some(java.sql.Timestamp.valueOf("1991-05-11 00:00:00"))),
    AccToCusRaw(Some(187851146),Some(146504598),Some(java.sql.Timestamp.valueOf("1992-08-12 00:00:00")),Some(java.sql.Timestamp.valueOf("2001-08-23 00:00:00")),Some(java.sql.Timestamp.valueOf("1992-08-12 00:00:00")),Some(java.sql.Timestamp.valueOf("2001-08-23 00:00:00")),Some("CUST ACCT LINK"),Some("US"),Some(java.sql.Timestamp.valueOf("1992-08-12 00:00:00"))),
    AccToCusRaw(Some(958151233),Some(169013008),Some(java.sql.Timestamp.valueOf("1996-03-11 00:00:00")),Some(java.sql.Timestamp.valueOf("2009-04-09 00:00:00")),Some(java.sql.Timestamp.valueOf("1996-03-11 00:00:00")),Some(java.sql.Timestamp.valueOf("2009-04-09 00:00:00")),Some("CUST ACCT LINK"),Some("CH"),Some(java.sql.Timestamp.valueOf("1996-03-11 00:00:00")))
  ).toDS.as[AccToCusRaw]

  val accountUpdateData = List(
    AccountRaw(Some(519209733),Some("Fireball Imports KGaA"),Some("2971 Courtright Street York ND 58386"),Some("Active"),Some("York"),Some("FANNIE MARTER"),Some("Fireball Imports KGaA"),None,None,Some("2971 Courtright Street"),Some("York"),Some("ND"),Some("58386"),Some("US"),Some(3353),Some("f"),None,Some("701-592-9885")),
    AccountRaw(Some(146504598),Some("Parklane Hosiery Imports Corp"),Some("4856 Nelson Street Wawa ON P0S 1K0"),Some("Active"),Some("Wawa"),Some("FLOR RULON"),Some("Parklane Hosiery Imports Corp"),None,None,Some("4856 Nelson Street"),Some("Wawa"),Some("ON"),Some("P0S 1K0"),Some("CA"),Some(7017),Some("f"),None,Some("705-856-6535")),
    AccountRaw(Some(169013008),Some("Hughes Markets Imports VOF"),Some("Gerbiweg 35 Buch bei Fra 8524"),Some("Active"),Some("Buch bei Fra"),Some("KOURTNEY DONAWAY"),Some("Hughes Markets Imports VOF"),None,None,Some("Gerbiweg 35"),Some("Buch bei Fra"),None,Some("8524"),Some("CH"),Some(88),Some("f"),None,None)
  ).toDS.as[AccountRaw]

}
