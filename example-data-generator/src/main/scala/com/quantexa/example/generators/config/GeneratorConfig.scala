package com.quantexa.example.generators.config

case class GeneratorConfig (
                             numberOfCustomerDocuments : Option[Int],
                             numberOfAccountDocuments: Option[Int],
                             numberOfHotlistDocuments: Option[Int],
                             numberOfTransactionDocuments: Option[Int],
                             numberOfPersonEntities: Option[Int],
                             numberOfBusinessEntities: Option[Int],
                             numberOfAddressEntities: Option[Int],
                             numberOfPhoneEntities: Option[Int],
                             numberOfEmailEntities: Option[Int],
                             rootHDFSPath : String,
                             familyNameChanged: Option[Double],
                             isEmployee: Option[Double],
                             amlAlrFlagged: Option[Double],
                             accountStates: String,
                             dataSourceCodes: String,
                             accountTypes: String,
                             customerState: String,
                             possibleGenders: String,
                             countryCodes: String,
                             transactionCurrencies: String,
                             transactionTypes: String,
                             seed: Option[Int]
                           )