package com.quantexa.example.etl.projects.fiu.customer

import com.quantexa.etl.compounds.CompoundCreatorIncremental
import com.quantexa.example.model.fiu.customer.CustomerExtractor
import com.quantexa.example.model.fiu.customer.CustomerModel.Customer


/***
  * Compound creator used to output the compounds defined in the model using our cleansed case class
  * Input: DocumentDataModel/CleansedDocumentDataModel.parquet
  * Output: Compounds/DocumentIndexInput.parquet
  *
  * We use the Quantexa compound creator to output the compounds defined in the model ready for loading into Elastic or running an ENG build
  *
  */
object CreateCompounds extends CompoundCreatorIncremental[Customer](
  CustomerExtractor.getClass.getCanonicalName, "customer", extractElements = true)(_.customerIdNumber.toString)
