package com.quantexa.example.etl.projects.fiu.transaction

import com.quantexa.example.etl.projects.fiu.transaction.helpers.CompoundCreator
import com.quantexa.example.model.fiu.transaction.TransactionExtractor
import com.quantexa.example.model.fiu.transaction.TransactionModel.AggregatedTransaction

/***
  * Compound creator used to output the compounds defined in the model using our aggregated cleansed case class
  * Input: DocumentDataModel/AggregatedCleansedDocumentDataModel.parquet
  * Output: Compounds/DocumentIndexInput.parquet and Compounds/EntityTypes.parquet
  *
  * Stage 5
  * We use the Quantexa compound creator to output the compounds defined in the model ready for loading into Elastic or running an ENG build
  *
  */
object CreateCompounds extends CompoundCreator[AggregatedTransaction](
  TransactionExtractor.getClass.getCanonicalName, "transaction", extractElements = true
)(_.aggregatedTransactionId.toString)
