package com.quantexa.example.etl.projects.fiu.utils

import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.util.metrics.elastic.ETLMetricsElasticLoader
import com.quantexa.scriptrunner.util.metrics.report.{ETLMetricsReportWriterBuilder, ElasticWriter}
import org.apache.log4j.Logger

object Metrics {
  val logger = Logger.getLogger("quantexa-spark-script")

  val repo = ETLMetricsRepository.builder()
    .setLogger(logger)
    .setAppName("IntegrationTest")
    .setReportWriterBuilder(new ETLMetricsReportWriterBuilder() {
      override def elasticWriter(elasticConfigPath: String): ElasticWriter = new ElasticWriter {
        override def elasticConfigPath: String = ""

        override def metricsLoader: ETLMetricsElasticLoader = ???
      }
    })
    .setAppId("IntegrationTest")
    .setStartTime(System.currentTimeMillis())
    .setUser("testUser")
    .build()
}