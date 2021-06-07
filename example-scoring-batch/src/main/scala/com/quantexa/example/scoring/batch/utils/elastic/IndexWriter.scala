package com.quantexa.example.scoring.batch.utils.elastic

import com.quantexa.elastic.client.QElasticClient
import com.quantexa.etl.core.elastic.{ElasticLoaderSettings, ElasticLoaderUpdateSettings, IndexWriterUtils}
import org.apache.commons.logging.{Log, LogFactory}
import org.apache.spark.TaskContext
import org.elasticsearch.hadoop.cfg.Settings
import org.elasticsearch.hadoop.rest.RestService

case class IndexWriter [T <: Product] (
                                   esHadoopSettingsForSearch: String,
                                   loaderSettings: ElasticLoaderSettings,
                                   clientProvider: (ElasticLoaderSettings, String) => QElasticClient,
                                   documentIdField: String) extends Serializable{

  //Index names
  val searchIndexName: String = loaderSettings.searchIndexName

  //Load settings
  val loadSearch = true

  //Fields prefixed with @ are done so to avoid clashing with actual case class fields in the model (T); they are not written to ES.
  val DEFAULT_INDEX_MAPPING_TYPE = "default"

  @transient lazy val log: Log = LogFactory.getLog(this.getClass)

  lazy val documentSettings: Settings = IndexWriterUtils.getSettings(
    hadoopSettings = esHadoopSettingsForSearch,
    indexPattern = s"$searchIndexName/$DEFAULT_INDEX_MAPPING_TYPE",
    mappingId = documentIdField,
    logger = log)

  val isUpdate: Boolean = loaderSettings.isInstanceOf[ElasticLoaderUpdateSettings]

  def writeTask(taskContext: TaskContext, data: Iterator[T]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val searchDocumentWriter = Some(RestService.createWriter(documentSettings, taskContext.partitionId, -1, log))
    val rows = data.toSeq

    taskContext.addTaskCompletionListener(TaskContext => {
      searchDocumentWriter.foreach(_.close)
    })

    rows.foreach {row =>
      val searchWithMeta = IndexWriterUtils.addFieldsToCaseClass(row, Map.empty)
      searchDocumentWriter.foreach(_.repository.writeToIndex(searchWithMeta))
    }
  }

}
