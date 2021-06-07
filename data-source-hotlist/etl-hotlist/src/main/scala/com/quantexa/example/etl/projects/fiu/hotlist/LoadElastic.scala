package com.quantexa.example.etl.projects.fiu.hotlist

import com.quantexa.etl.elastic.ElasticLoadScript
import com.quantexa.etl.core.elastic.Elastic6LoaderCombined
import com.quantexa.example.model.fiu.hotlist.HotlistModel.Hotlist

/***
  * Used to load the compounds / search documents into ElasticSearch
  * Input: Compounds/DocumentIndexInput.parquet
  *
  * Stage 6
  * We use the Quantexa Elastic Loader to load the the compounds and search documents into ElasticSearch
  *
  */

object LoadElastic extends ElasticLoadScript[Hotlist](documentType = "hotlist", loader = new Elastic6LoaderCombined())
