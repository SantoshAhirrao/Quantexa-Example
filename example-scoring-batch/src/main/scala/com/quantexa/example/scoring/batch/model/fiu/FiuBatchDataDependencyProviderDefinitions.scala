package com.quantexa.example.scoring.batch.model.fiu

import com.quantexa.example.scoring.batch.scores.fiu.integration.{RollupTxnCustomerDateScoresToCustomerLevel, RollupTxnScoreToCustomerLevel}
import com.quantexa.example.scoring.model.fiu.FiuDataDependencyProviderDefinitions
import com.quantexa.example.scoring.model.fiu.RollupModel.{CustomerDateScoreOutput, CustomerRollup, TransactionScoreOutput}
import com.quantexa.example.scoring.utils.TypedConfigReader.ProjectExampleConfig
import com.quantexa.scoring.framework.model.DataDependencyModel.{DataDependencyProviderDefinitions, DefaultDataDependencyProviderDefinition, ScalaDataDependencyProviderDefinition, SparkDataDependencyProviderDefinition}

case class FiuBatchDataDependencyProviderDefinitions(config:ProjectExampleConfig) extends DataDependencyProviderDefinitions {

  import scala.reflect.runtime.universe.{TypeTag, typeOf}

  def getDataDependencyProvider[T](implicit tt: TypeTag[T]): SparkDataDependencyProviderDefinition
    with ScalaDataDependencyProviderDefinition = {
    tt.tpe match {
      case x if x <:< typeOf[CustomerRollup[TransactionScoreOutput]] => DefaultDataDependencyProviderDefinition(
        path = RollupTxnScoreToCustomerLevel.outputFilePath,
        format = "parquet",
        indexName = null,
        documentType = null)
      case x if x <:< typeOf[CustomerRollup[CustomerDateScoreOutput]] => DefaultDataDependencyProviderDefinition(
        path = RollupTxnCustomerDateScoresToCustomerLevel.outputFilePath,
        format = "parquet",
        indexName = null,
        documentType = null)
      case _ => FiuDataDependencyProviderDefinitions(config).getDataDependencyProvider // Make a call to the real-time DDP
    }
  }
}
