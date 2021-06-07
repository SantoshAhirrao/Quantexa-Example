package com.quantexa.example.model.fiu.transaction

import java.sql.Date

import com.quantexa.example.model.fiu.transaction.TransactionModel._
import com.quantexa.example.model.fiu.transaction.TransactionModel.Traversals._
import com.quantexa.model.core.datasets.ParsedDatasets.{LensParsedBusiness, LensParsedIndividualName, StandardParsedElements}
import com.quantexa.model.core.datasets.ParsedDatasets.StandardParsedElements.{businessNameClean, forename, surname}
import com.quantexa.resolver.ingest.Model._


object Compounds {
  import com.quantexa.resolver.ingest.BrokenLenses.BOpticOps._

  /////////////////////////////////////// Document Attributes //////////////////////////////////////////////

  val originatorName          = DocumentAttributeDefinition(traversal[AggregatedTransaction, String](_.originatorName))
  val aggregatedTransactionId = DocumentAttributeDefinition(lens[AggregatedTransaction, String](_.aggregatedTransactionId))
  val beneficiaryName         = DocumentAttributeDefinition(traversal[AggregatedTransaction, String](_.beneficiaryName))
  val crossBorderFlag         = DocumentAttributeDefinition(traversal[AggregatedTransaction, Boolean](_.crossBorderFlag))
  val debitCredit             = DocumentAttributeDefinition(traversal[AggregatedTransaction, String](_.debitCredit))
  val currencies              = DocumentAttributeDefinition(traversal[AggregatedTransaction, String](_.currencies))

  val txnSumTotal            = DocumentAttributeDefinition(Traversals.aggTxnToTotalStats ~> Traversals.aggTxnToSum)
  val txnCountTotal          = DocumentAttributeDefinition(Traversals.aggTxnToTotalStats ~> Traversals.aggTxnToCount)
  val txnBaseSumTotal        = DocumentAttributeDefinition(Traversals.aggTxnToTotalStats ~> Traversals.aggTxnToBaseSum)

  val firstDate               = DocumentAttributeDefinition(traversal[AggregatedTransaction, java.sql.Date](_.firstDate.map( ts => new Date(ts.getTime))))
  val lastDate                = DocumentAttributeDefinition(traversal[AggregatedTransaction, java.sql.Date](_.lastDate.map(ts => new Date(ts.getTime))))


  ///////////////////////////////////////// Individual Compounds /////////////////////////////////////////

  import StandardParsedElements._
  import Lens._
  import Elements._

  val individualCompounds = CompoundDefinitionList(
    "individual",
    NamedCompound("forename_surname_accountId",
      (forename[AggregatedTransactionParty] ~ surname[AggregatedTransactionParty]) ~ (individualNameToAggTxnAccount ~> accountId))
  )

  //////////////////////////////////////// Individual Attributes /////////////////////////////////////////

  val individualAttributeDefinitions = EntityAttributeDefinitionList(
    "individual",
    NamedEntityAttribute("individualDisplay",
      traversal[LensParsedIndividualName[AggregatedTransactionParty], String](_.nameDisplay)),
    NamedEntityAttribute("txnSumTotal",
      partyIndividualToAggTrn ~> Traversals.aggTxnToTotalStats ~> Traversals.aggTxnToSum)

  )

  /////////////////////////////////////// Business Compounds ///////////////////////////////////////

  val businessCompounds = CompoundDefinitionList(
    "business",
    NamedCompound("businessNameClean_accountId", (
      businessNameClean[AggregatedTransactionParty]) ~ (businessNameToAccount ~> accountId))
  )

  ////////////////////////////////////// Business Attributes ///////////////////////////////////////

  val businessAttributeDefinitions = EntityAttributeDefinitionList(
    "business",
    NamedEntityAttribute(
      "businessDisplay",
      traversal[LensParsedBusiness[AggregatedTransactionParty], String](_.businessDisplay))
  )

  ////////////////////////////////////// Account Compounds ////////////////////////////////////////

  val accountCompoundDefs = CompoundDefinitionList(
    "account",
    NamedCompound("accountId", accountId),
    NamedCompound("accountNumber", accountNumber)

    // TODO add sort code to transaction smoke data and corresponding ETL code
//    NamedCompound("sortCode", sortCode)
    )

  ////////////////////////////////////// Account Attributes //////////////////////////////////////

  val accountAttributes = EntityAttributeDefinitionList(
    "account",
    NamedEntityAttribute("label", accountId),
    NamedEntityAttribute("accountNumber", accountNumber
    )
  )
}

