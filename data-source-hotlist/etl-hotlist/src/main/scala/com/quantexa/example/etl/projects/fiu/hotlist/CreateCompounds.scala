package com.quantexa.example.etl.projects.fiu.hotlist

import com.quantexa.etl.compounds.CompoundCreator
import com.quantexa.example.model.fiu.hotlist.HotlistExtractor
import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist

/***
  * Compound creator used to output the compounds defined in the model using our cleansed case class
  * Input: DocumentDataModel/CleansedDocumentDataModel.parquet
  * Output: Compounds/DocumentIndexInput.parquet and Compounds/EntityTypes.parquet
  *
  * Stage 5
  * We use the Quantexa compound creator to output the compounds defined in the model ready for loading into Elastic or running an ENG build
  *
  */

object CreateCompounds extends CompoundCreator[Hotlist](
  HotlistExtractor.getClass.getCanonicalName, "hotlist", extractElements = true)(_.hotlistId.toString)
