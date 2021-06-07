package com.quantexa.example.scoring.batch.model.fiu

import com.quantexa.example.model.fiu.customer.CustomerModel.Customer
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.DataDependencyModel.{DataDependencyProviderDefinitions, DefaultDataDependencyProviderDefinition, ScalaDataDependencyProviderDefinition, SparkDataDependencyProviderDefinition}

class FiuTestDataDependencyProviderDefinitions(config: ProjectExampleConfig) extends DataDependencyProviderDefinitions {

  import scala.reflect.runtime.universe.{TypeTag, typeOf}

  def getDataDependencyProvider[T](implicit tt: TypeTag[T]): SparkDataDependencyProviderDefinition
    with ScalaDataDependencyProviderDefinition = {

    tt.tpe match {
      case x if x <:< typeOf[Customer] => DefaultDataDependencyProviderDefinition(
        path = config.hdfsFolder + "/DocumentDataModel/CleansedDocumentDataModel.parquet",
        format = "parquet",
        indexName = null,
        documentType = null)
    }
  }
}