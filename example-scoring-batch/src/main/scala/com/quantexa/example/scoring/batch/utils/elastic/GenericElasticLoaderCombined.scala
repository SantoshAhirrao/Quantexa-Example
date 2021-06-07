package com.quantexa.example.scoring.batch.utils.elastic

import com.quantexa.elastic.loader.utils.settings.ElasticDataModelSettings
import com.quantexa.etl.core.elastic.ElasticLoaderSettings
import com.quantexa.etl.core.utils.metrics.{JobMetrics, JobMetricsNoOp}
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import scala.reflect.runtime.universe.TypeTag

trait GenericElasticLoaderCombined {
  def load[T <: Product : TypeTag](spark: SparkSession,
                                   logger: Logger,
                                   documentType: String,
                                   documentPath: String,
                                   settings: ElasticLoaderSettings,
                                   mappingSettings: ElasticDataModelSettings[T],
                                   documentIdField: String,
                                   metrics: JobMetrics = JobMetricsNoOp): Unit
}
