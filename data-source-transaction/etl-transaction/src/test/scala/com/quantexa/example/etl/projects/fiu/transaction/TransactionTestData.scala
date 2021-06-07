package com.quantexa.example.etl.projects.fiu.transaction

import java.sql.{Date, Timestamp}
import com.quantexa.example.etl.projects.fiu.utils.SparkTestSession.spark.sqlContext.implicits._
import com.quantexa.example.model.fiu.customer.CustomerRawModel.AccToCusRaw
import com.quantexa.example.model.fiu.transaction.TransactionModel.Transaction
import com.quantexa.example.model.fiu.transaction.TransactionRawModel.TransactionRaw


object TransactionTestData {
  val testDate = Date.valueOf("2017-01-01")
  val testTimestamp = Timestamp.valueOf(testDate.toLocalDate.atStartOfDay)

  val testTransaction = Transaction(
    transactionId = "07ff6c1aa185074cd0ac3c6d8ff819cb",
    originatingAccount = None,
    beneficiaryAccount = None,
    debitCredit = "D",
    postingDate = testTimestamp,
    runDate = testDate,
    txnAmount = 4000,
    txnCurrency = "Stirling",
    txnAmountInBaseCurrency = 4000,
    baseCurrency = "Stirling",
    txnDescription = None,
    sourceSystem = None,
    metaData = None
  )

  val transactionData = List(
    TransactionRaw(Some("0830cee344f25916be0b38fbd2a58839"),Some(215477692),Some(799696617),Some("WIZZY NETWORKS LLC"),None,None,Some("TOM BURGER"),Some("Stadionstrasse 17, RECHNITZ, Burgenland, 7471"),Some("AT"),Some(java.sql.Timestamp.valueOf("2017-05-17 01:00:00.000")),Some(15.9),Some("POINT OF SALE"),Some("WIZZY NETWORKS LLC"),Some("D"),Some("AT"),Some("AT"),Some(15.9),Some(java.sql.Timestamp.valueOf("2017-05-17 01:00:00.000")),Some("TXN"),Some(1428452549),Some("Stirling"),Some("Stirling")),
    TransactionRaw(Some("08b493e6712427fe0a5988daded49afb"),Some(444535634),Some(897044149),Some("AIDEN WUNDERLICH"),Some("AU"),Some("69 Benny Street, DEVONPORT, TAS, 7310"),Some("WIDGETS PLC"),None,Some("AT"),Some(java.sql.Timestamp.valueOf("2017-01-13 01:32:46.659")),Some(199.75),Some("MANUAL TRANSFER"),Some("AIDEN WUNDERLICH (MANUAL TRANSFER)"),Some("D"),Some("AU"),Some("AT"),Some(199.75),Some(java.sql.Timestamp.valueOf("2017-01-13 01:32:46.659")),Some("TXN"),Some(1428467025),Some("Stirling"),Some("Stirling")),
    TransactionRaw(Some("08ff4a7d0955f690dea533ff82148788"),Some(264366057),Some(422301096),Some("Lazysize VOF"),Some("CA"),Some("1894 Islington Ave, Toronto, ON, M8V 3B6"),Some("TABITHA CARR"),None,Some("AT"),Some(java.sql.Timestamp.valueOf("2016-11-17 01:11:39.428")),Some(40.77),Some("DIRECT DEBIT"),Some("Lazysize VOF (DIRECT DEBIT)"),Some("D"),Some("CA"),Some("AT"),Some(40.77),Some(java.sql.Timestamp.valueOf("2016-11-17 01:11:39.428")),Some("TXN"),Some(1428485869),Some("Stirling"),Some("Stirling")),
    TransactionRaw(Some("0b034585d428a9355d2c900666a50324"),Some(215477692),Some(622152213),Some("XYZ PLC"),None,None,Some("Scott Ties Empresa"),Some("4456 Taylor Street, West Nyack, NY,10994"),Some("US"),Some(java.sql.Timestamp.valueOf("2017-05-01 01:00:00.000")),Some(62.32),Some("POINT OF SALE"),Some("XYZ PLC"),Some("D"),Some("AT"),Some("US"),Some(62.32),Some(java.sql.Timestamp.valueOf("2017-05-01 01:00:00.000")),Some("TXN"),Some(1428467585),Some("Stirling"),Some("Stirling")),
    TransactionRaw(Some("0dcc6a2ff4283e6ff0f0963d49126384"),Some(456215878),Some(139407504),Some("GEOFF'S CARS LTD"),Some("AT"),None,Some("DEWEY AUBUCHON"),Some("2480 Hillside Dr, Elliot Lake, ON, P5A 1X5"),Some("CA"),Some(java.sql.Timestamp.valueOf("2016-12-30 09:16:33.182")),Some(48.55),Some("CASH CREDIT"),Some("GEOFF'S CARS LTD (CASH CREDIT)"),Some("C"),Some("AT"),Some("CA"),Some(48.55),Some(java.sql.Timestamp.valueOf("2016-12-30 09:16:33.182")),Some("TXN"),Some(1428473023),Some("Stirling"),Some("Stirling")),
    TransactionRaw(Some("1152067ce9d347b1c8a489fcbf753c38"),Some(963555770),Some(440673220),None,None,None,Some("BOXES LTD"),None,Some("AT"),Some(java.sql.Timestamp.valueOf("2016-10-04 05:43:58.751")),Some(32.31),Some("CHEQUE CREDIT"),Some("CHEQUE CREDIT"),Some("C"),Some("DE"),Some("AT"),Some(32.31),Some(java.sql.Timestamp.valueOf("2016-10-04 05:43:58.751")),Some("TXN"),Some(1428448202),Some("Stirling"),Some("Stirling")),
    TransactionRaw(Some("122c44e097b73dc3b552c253404187db"),Some(421792524),Some(448900540),Some("PARMENIO SOTELO"),Some("ES"),Some("Ctra. de Fuentenueva 95 ,El Berrueco Madrid 28192"),Some("MARCEL KUSTER"),Some("Reeperbahn 66, Treuen, SN, 08229"),Some("DE"),Some(java.sql.Timestamp.valueOf("2017-01-03 01:35:12.270")),Some(70.8),Some("DIRECT DEBIT"),Some("PARMENIO SOTELO (DIRECT DEBIT)"),Some("D"),Some("ES"),Some("DE"),Some(70.8),Some(java.sql.Timestamp.valueOf("2017-01-03 01:35:12.270")),Some("TXN"),Some(1428496768),Some("Stirling"),Some("Stirling"))
  ).toDS.as[TransactionRaw]

  val transactionUpdateData = List(
    TransactionRaw(Some("13d3ab2510470aed22c82ed665780c9f"),Some(437257054),Some(213737285),Some("JEREMY JONES"),Some("AT"),None,Some("JOHN MACHADO"),Some("890 Tuna Street, Southfield, MI, 48075"),Some("US"),Some(java.sql.Timestamp.valueOf("2017-05-14 03:27:35.247")),Some(95.09),Some("FASTER PAYMENT"),Some("JEREMY JONES (FASTER PAYMENT)"),Some("D"),Some("AT"),Some("US"),Some(95.09),Some(java.sql.Timestamp.valueOf("2017-05-14 03:27:35.247")),Some("TXN"),Some(1428449505),Some("Stirling"),Some("Stirling")),
    TransactionRaw(Some("1415c9ec65ae53009ad437872ca4c994"),Some(120828090),Some(496831792),Some("Baltimore Markets gomei-kaisha"),Some("NO"),Some("Neptunveien 12, OSLO, 0493"),Some("TABITHA CARR"),None,Some("AT"),Some(java.sql.Timestamp.valueOf("2017-03-01 01:43:21.396")),Some(78.7512),Some("MANUAL TRANSFER"),Some("Baltimore Markets gomei-kaisha (MANUAL TRANSFER)"),Some("D"),Some("NO"),Some("AT"),Some(69.08),Some(java.sql.Timestamp.valueOf("2017-03-01 01:43:21.396")),Some("TXN"),Some(1428402026),Some("Euro"),Some("Stirling")),
    TransactionRaw(Some("14c5f2f19500f0066d5071f07b660fea"),Some(131608423),Some(233824284),Some("TEARLACH CHALOUX"),Some("BE"),Some("Stationsstraat 357, Itterbeek VBR 1701"),Some("GEOFF'S CARS LTD"),None,Some("AT"),Some(java.sql.Timestamp.valueOf("2017-01-23 06:20:34.568")),Some(6.46),Some("ELECTRONIC TRANSFER"),Some("TEARLACH CHALOUX (ELECTRONIC TRANSFER)"),Some("D"),Some("BE"),Some("AT"),Some(6.46),Some(java.sql.Timestamp.valueOf("2017-01-23 06:20:34.568")),Some("TXN"),Some(1428499714),Some("Stirling"),Some("Stirling"))
  ).toDS.as[TransactionRaw]

  val accountToCustomerData = List(
    AccToCusRaw(Some(130585981),Some(799696617),Some(java.sql.Timestamp.valueOf("1990-12-05 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-11-14 00:00:00")),Some(java.sql.Timestamp.valueOf("1990-12-05 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-11-14 00:00:00")),Some("CUST ACCT LINK"),Some("AU"),Some(java.sql.Timestamp.valueOf("1990-12-05 00:00:00"))),
    AccToCusRaw(Some(830025268),Some(897044149),Some(java.sql.Timestamp.valueOf("1998-11-21 00:00:00")),None,Some(java.sql.Timestamp.valueOf("1998-11-21 00:00:00")),None,Some("CUST ACCT LINK"),Some("NL"),Some(java.sql.Timestamp.valueOf("1998-11-21 00:00:00"))),
    AccToCusRaw(Some(244237377),Some(422301096),Some(java.sql.Timestamp.valueOf("2006-04-21 00:00:00")),None,Some(java.sql.Timestamp.valueOf("2006-04-21 00:00:00")),None,Some("CUST ACCT LINK"),Some("ZA"),Some(java.sql.Timestamp.valueOf("2006-04-21 00:00:00"))),
    AccToCusRaw(Some(986113362),Some(622152213),Some(java.sql.Timestamp.valueOf("2009-10-10 00:00:00")),None,Some(java.sql.Timestamp.valueOf("2009-10-10 00:00:00")),None,Some("CUST ACCT LINK"),Some("AU"),Some(java.sql.Timestamp.valueOf("2009-10-10 00:00:00"))),
    AccToCusRaw(Some(535525763),Some(139407504),Some(java.sql.Timestamp.valueOf("1999-09-07 00:00:00")),Some(java.sql.Timestamp.valueOf("2006-07-15 00:00:00")),Some(java.sql.Timestamp.valueOf("1999-09-07 00:00:00")),Some(java.sql.Timestamp.valueOf("2006-07-15 00:00:00")),Some("CUST ACCT LINK"),Some("US"),Some(java.sql.Timestamp.valueOf("1999-09-07 00:00:00"))),
    AccToCusRaw(Some(633910089),Some(440673220),Some(java.sql.Timestamp.valueOf("1991-12-03 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-08-18 00:00:00")),Some(java.sql.Timestamp.valueOf("1991-12-03 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-08-18 00:00:00")),Some("CUST ACCT LINK"),Some("AU"),Some(java.sql.Timestamp.valueOf("1991-12-03 00:00:00"))),
    AccToCusRaw(Some(633910089),Some(448900540),Some(java.sql.Timestamp.valueOf("1991-12-03 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-08-18 00:00:00")),Some(java.sql.Timestamp.valueOf("1991-12-03 00:00:00")),Some(java.sql.Timestamp.valueOf("2000-08-18 00:00:00")),Some("CUST ACCT LINK"),Some("AU"),Some(java.sql.Timestamp.valueOf("1991-12-03 00:00:00")))
  ).toDS.as[AccToCusRaw]

  val accountToCustomerUpdateData = List(
    AccToCusRaw(Some(905790248),Some(213737285),Some(java.sql.Timestamp.valueOf("1991-05-11 00:00:00")),Some(java.sql.Timestamp.valueOf("1995-03-02 00:00:00")),Some(java.sql.Timestamp.valueOf("1991-05-11 00:00:00")),Some(java.sql.Timestamp.valueOf("1995-03-02 00:00:00")),Some("CUST ACCT LINK"),Some("AU"),Some(java.sql.Timestamp.valueOf("1991-05-11 00:00:00"))),
    AccToCusRaw(Some(187851146),Some(496831792),Some(java.sql.Timestamp.valueOf("1992-08-12 00:00:00")),Some(java.sql.Timestamp.valueOf("2001-08-23 00:00:00")),Some(java.sql.Timestamp.valueOf("1992-08-12 00:00:00")),Some(java.sql.Timestamp.valueOf("2001-08-23 00:00:00")),Some("CUST ACCT LINK"),Some("US"),Some(java.sql.Timestamp.valueOf("1992-08-12 00:00:00"))),
    AccToCusRaw(Some(958151233),Some(233824284),Some(java.sql.Timestamp.valueOf("1996-03-11 00:00:00")),Some(java.sql.Timestamp.valueOf("2009-04-09 00:00:00")),Some(java.sql.Timestamp.valueOf("1996-03-11 00:00:00")),Some(java.sql.Timestamp.valueOf("2009-04-09 00:00:00")),Some("CUST ACCT LINK"),Some("CH"),Some(java.sql.Timestamp.valueOf("1996-03-11 00:00:00")))
  ).toDS.as[AccToCusRaw]

}
