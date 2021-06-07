package com.quantexa.example.model.fiu.transaction

import com.quantexa.resolver.ingest.Model.{CompoundDefinitionList, DocumentAttributeDefinition, NamedCompound}
import com.quantexa.model.core.datasets.ParsedDatasets.{StandardParsedAttributes => stdAttrs, StandardParsedCompounds => stdCompounds, StandardParsedElements => stdElems}
import com.quantexa.model.core.datasets.ParsedDatasets._
import com.quantexa.resolver.ingest.Model
import com.quantexa.resolver.ingest.Model._
import com.quantexa.resolver.ingest.WithParent.WithParent
import com.quantexa.resolver.ingest.extractors.DataModelExtractor
import com.quantexa.scriptrunner.util.incremental.MetaDataModel.MetadataRunId
import shapeless.Typeable

/***
  * Case class models used to represent the hierarchical model for the FIU smoke test data
  *
  * Stage 2
  * At this stage we should perform data quality checks on our raw data. Profile the data and make
  * decisions as to what our model should be. We should decide what values to nullify and what to do with data
  * that doesnt fit our model
  */
object TransactionModel {

  /**
    * Case class to represent a transfer of money between two Accounts
    *
    * @param transactionId                Primary key
    * @param originatingAccount           Source Account
    * @param beneficiaryAccount           Target Account
    * @param debitCredit                  Flag to indicate debit or credit from originator
    * @param postingDate                  Date the transaction was posted
    * @param runDate                      Date the transaction was processed
    * @param txnAmount                    Normalized Amount
    * @param txnCurrency                  Currency of the transaction
    * @param txnAmountInBaseCurrency      Amount in the base currency
    * @param baseCurrency                 Currency of the originating account
    * @param txnDescription               General description field
    * @param sourceSystem                 Source System ("TXN" placeholder in smoke data)
    * @param metaData                     Metadata
    */
  case class Transaction(
                   @id transactionId: String,
                   originatingAccount: Option[TransactionAccount],
                   beneficiaryAccount: Option[TransactionAccount],
                   debitCredit: String,
                   postingDate: java.sql.Timestamp,
                   runDate: java.sql.Date,
                   txnAmount: Double,
                   txnCurrency: String,
                   txnAmountInBaseCurrency: Double,
                   baseCurrency: String,
                   txnDescription: Option[String],
                   sourceSystem: Option[String],
                   metaData: Option[MetadataRunId])


  /**
    * Case class to represent a Transaction Account
    *
    * @param accountId                    Account ID primary key
    * @param accountNumber                Account Number
    * @param accountName                  Account Name
    * @param alternateAccountNumber       Alternate Account Number
    * @param accountIban                  International Bank Account Number
    * @param accountCurrency              The base currency for the account
    * @param accountCountryISO3Code       Country in ISO3 format
    * @param accountCountry               Country
    * @param productType                  Product Type eg. ATM withdrawal, Cheque, PoS, etc
    */
  case class TransactionAccount(
                                 @id accountId: String,
                                 accountNumber: String,
                                 accountName: Option[String],
                                 alternateAccountNumber: Option[String],
                                 accountIban: Option[String],
                                 accountCurrency: Option[String],
                                 accountCountryISO3Code: Option[String],
                                 accountCountry: Option[String],
                                 productType: Option[String])

  /*-------------------------------------------------
  // AGGREGATED CASE CLASSES
  --------------------------------------------------*/

  /**
    * Case class to represent the aggregation of a number of transactions between
    * two common parties with statistics by age group (from baseline of last
    * transaction posting date)
    *
    * @param aggregatedTransactionId      Concatenation of originating and beneficiary account IDs
    * @param originatorAccount            Originating account including transaction party
    * @param beneficiaryAccount           Beneficiary account including transaction party
    * @param originatorName               Originator name
    * @param beneficiaryName              Beneficiary name
    * @param crossBorderFlag              Flag indicating the transaction was between countries
    * @param debitCredit                  Flag indicating transaction was either debit or credit
    * @param currencies                   List of currencies used in the transactions
    * @param txnStatsTotal                Statistics for total aggregate transactions
    * @param firstDate                    Posting Date of first transaction in aggregation
    * @param lastDate                     Posting Date of last transaction in aggregation
    */
  case class AggregatedTransaction(
                             @id aggregatedTransactionId: String,
                             customerIdNumber: Long, //ID is not optional
                             originatorAccount: AggregatedTransactionAccount,
                             beneficiaryAccount: AggregatedTransactionAccount,
                             originatorName: Option[String],
                             beneficiaryName: Option[String],
                             crossBorderFlag: Option[Boolean],
                             debitCredit: Option[String],
                             currencies: Seq[String],
                             txnStatsTotal: Option[AggregatedStats],
                             firstDate: Option[java.sql.Date],
                             lastDate: Option[java.sql.Date])

  /**
    * Case class to represent the end points for an Aggregation of Transactions
    *
    * @param accountId                    Account ID (primary key)
    * @param accountNumber                Account Number
    * @param accountSortCode              Sort Code
    * @param accountIban                  International Bank Account Number
    * @param cleansedAccountNumber        Cleansed Account Number
    * @param cleansedSortCode             Cleansed Sort Code
    * @param accountBic                   Account Bank Identifier Code
    * @param bankCountry                  Local Country of Bank
    * @param originBeneficiary            Beneficiary
    * @param transactionParty             The Individual or Business holding the account
    */
  case class AggregatedTransactionAccount(
                                           @id accountId: String,
                                           accountNumber: String,
                                           accountSortCode: Option[String],
                                           accountIban: Option[String],
                                           cleansedAccountNumber: Option[String],
                                           cleansedSortCode: Option[String],
                                           accountBic: Option[String],
                                           bankCountry: Option[String],
                                           originBeneficiary: Option[String],
                                           transactionParty: Option[AggregatedTransactionParty]
                                         ) extends WithParent[AggregatedTransaction]

  object AggregatedTransactionAccount {
    def apply(accountId: String,
              accountNumber: String,
              aggregatedTransactionParty: AggregatedTransactionParty): AggregatedTransactionAccount = {
      AggregatedTransactionAccount(
        accountId = accountId,
        accountNumber = accountNumber,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        transactionParty = Some(aggregatedTransactionParty)
      )
    }
  }

  /**
    * Case class representing the individual or business holding the account
    *
    * @param partyId                      Party ID primary key
    * @param partyName                    Party name
    * @param parsedIndividualNames        Names of the Individual
    * @param parsedBusinessNames          Names of the Business
    */
  case class AggregatedTransactionParty(
                           @id partyId: String,
                           partyName: Option[String],
                           parsedIndividualNames: Option[Seq[LensParsedIndividualName[AggregatedTransactionParty]]],
                           parsedBusinessNames: Option[LensParsedBusiness[AggregatedTransactionParty]]
                         ) extends WithParent[AggregatedTransactionAccount]

  /**
    * Case class for statistics over the aggregation
    *
    * @param txnSum                       Sum of normalized values
    * @param txnCount                     Count of transactions
    * @param txnMin                       Min of normalized values
    * @param txnMax                       Max of normalized values
    * @param txnSumBase                   Sum of base values
    * @param txnMinBase                   Min of base values
    * @param txnMaxBase                   Max of base values
    */
  case class AggregatedStats(
                              txnSum: Option[Double],
                              txnCount: Option[Long],
                              txnMin: Option[Double],
                              txnMax: Option[Double],
                              txnSumBase: Option[Double],
                              txnMinBase: Option[Double],
                              txnMaxBase: Option[Double]
                            )

  object AggregatedTransaction {

    import com.quantexa.security.shapeless.Redact
    import com.quantexa.security.shapeless.Redact.Redactor

    val redactor: Redactor[AggregatedTransaction] = Redact.apply[AggregatedTransaction]
  }

  object Traversals {
    import com.quantexa.resolver.ingest.BrokenLenses._
    import com.quantexa.resolver.ingest.BrokenLenses.BOpticOps._

    val aggTxnToTotalStats   = traversal[AggregatedTransaction, AggregatedStats](_.txnStatsTotal)

    private def roundAt(p: Int, n: Option[Double]): Option[Double] = n match {
      case Some(d) => {
        val s = math.pow(10, p)
        Some(math.round(d * s) / s)
      }
      case None => Option.empty
    }

    val aggTxnToSum           = traversal[AggregatedStats, Double](as => roundAt(2, as.txnSum))
    val aggTxnToCount         = traversal[AggregatedStats, Long]  (_.txnCount)
    val aggTxnToMin           = traversal[AggregatedStats, Double](as => roundAt(2, as.txnMin))
    val aggTxnToMax           = traversal[AggregatedStats, Double](as => roundAt(2, as.txnMax))
    val aggTxnToBaseSum       = traversal[AggregatedStats, Double](as => roundAt(2, as.txnSumBase))
    val aggTxnToBaseMin       = traversal[AggregatedStats, Double](as => roundAt(2, as.txnMinBase))
    val aggTxnToBaseMax       = traversal[AggregatedStats, Double](as => roundAt(2, as.txnMaxBase))

    val partyIndividualToAggTrn = lens[LensParsedIndividualName[AggregatedTransactionParty], AggregatedTransaction] {
      name => name.parent.parent.parent
    }
  }

  object Lens {
    import com.quantexa.resolver.ingest.BrokenLenses._
    import com.quantexa.resolver.ingest.BrokenLenses.BOpticOps._

    val individualNameToAggTxnAccount = lens[LensParsedIndividualName[AggregatedTransactionParty], AggregatedTransactionAccount](x => x.parent.parent)
    val businessNameToAccount         = lens[LensParsedBusiness[AggregatedTransactionParty], AggregatedTransactionAccount](x => x.parent.parent)
  }

  implicit val TransactionTypeable = Typeable.simpleTypeable(classOf[TransactionModel.AggregatedTransaction])

}

import com.quantexa.example.model.fiu.transaction.TransactionModel.TransactionTypeable
object TransactionExtractor extends DataModelExtractor[TransactionModel.AggregatedTransaction](Compounds)