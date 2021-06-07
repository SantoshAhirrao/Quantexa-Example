package com.quantexa.example.scoring.model.fiu

import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist
import com.quantexa.example.model.fiu.lookupdatamodels.{HighRiskCountryCode, PostcodePrice}
import com.quantexa.example.model.fiu.transaction.TransactionModel.Transaction
import com.quantexa.example.scoring.model.fiu.FactsModel.{CustomerDateFacts, TransactionFacts}
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerDateScoreOutput, CustomerRollup, TransactionScoreOutput}
import com.quantexa.example.scoring.model.fiu.FactsModel.AggregatedTransactionFacts
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.DataDependencyModel.{DataDependencyProviderDefinitions, DefaultDataDependencyProviderDefinition, ScalaDataDependencyProviderDefinition, SparkDataDependencyProviderDefinition}

case class FiuDataDependencyProviderDefinitions(config:ProjectExampleConfig) extends DataDependencyProviderDefinitions {
  import scala.reflect.runtime.universe.{TypeTag, typeOf}

  def getDataDependencyProvider[T](implicit tt: TypeTag[T]): SparkDataDependencyProviderDefinition
    with ScalaDataDependencyProviderDefinition = {

    tt.tpe match {
      case x if x <:< typeOf[Customer]  => DefaultDataDependencyProviderDefinition(
        path = config.hdfsFolderCustomer + "/DocumentDataModel/CleansedDocumentDataModel.parquet",
        format = "parquet",
            indexName = null,
            documentType = null)
      case x if x <:< typeOf[Hotlist]  => DefaultDataDependencyProviderDefinition(
        path = config.hdfsFolderHotlist + "/DocumentDataModel/CleansedDocumentDataModel.parquet",
        format = "parquet",
            indexName = null,
            documentType = null)
      case x if x <:< typeOf[HighRiskCountryCode]  => DefaultDataDependencyProviderDefinition(
        path = config.highRiskCountryCodesLookup.path,
        format = config.highRiskCountryCodesLookup.format,
        indexName = config.highRiskCountryCodesLookup.indexName,
        documentType = config.highRiskCountryCodesLookup.documentType)
      case x if x <:< typeOf[PostcodePrice]  => DefaultDataDependencyProviderDefinition(
        path = config.postcodePricesLookup.path,
        format = config.postcodePricesLookup.format,
        indexName = config.postcodePricesLookup.indexName,
        documentType = config.postcodePricesLookup.documentType)
      case x if x <:< typeOf[CustomerRollup[TransactionScoreOutput]]  => DefaultDataDependencyProviderDefinition(
        path = config.txnScoreToCustomerRollup.path,
        format = config.txnScoreToCustomerRollup.format,
        indexName = config.txnScoreToCustomerRollup.indexName,
        documentType = config.txnScoreToCustomerRollup.documentType)
      case x if x <:< typeOf[CustomerRollup[CustomerDateScoreOutput]]  => DefaultDataDependencyProviderDefinition(
        path = config.txnCustomerDateScoreToCustomerRollup.path,
        format = config.txnCustomerDateScoreToCustomerRollup.format,
        indexName = config.txnCustomerDateScoreToCustomerRollup.indexName,
        documentType = config.txnCustomerDateScoreToCustomerRollup.documentType)
      case x if x <:< typeOf[Transaction]  => DefaultDataDependencyProviderDefinition(
        path = config.hdfsFolderTransactions + "/DocumentDataModel/CleansedDocumentDataModel.parquet",
        format = "parquet",
            indexName = null,
            documentType = null)
        //FIXME: IP-515 This shouldn't even be serialised? Check this and remove. IF not, location should sit within scoring framework
      case x if x <:< typeOf[TransactionFacts]  => DefaultDataDependencyProviderDefinition(
        path = config.hdfsFolderTransactionFacts,
        format = "parquet",
            indexName = null,
            documentType = null)
      case x if x <:< typeOf[CustomerDateFacts]  => DefaultDataDependencyProviderDefinition(
        path = config.hdfsFolderCustomerDateFacts,
        format = "parquet",
            indexName = null,
            documentType = null)
      case x if x <:< typeOf[AggregatedTransactionFacts]  => DefaultDataDependencyProviderDefinition(
        path = config.hdfsFolderAggregatedTransactionFacts,
        format = "parquet",
        indexName = null,
        documentType = null)
    }
  }
}