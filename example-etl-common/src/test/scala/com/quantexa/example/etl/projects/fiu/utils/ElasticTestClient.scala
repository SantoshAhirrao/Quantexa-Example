package com.quantexa.example.etl.projects.fiu.utils

import com.google.common.io.Files
import com.sksamuel.elastic4s.embedded.LocalNode
import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties}
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.Settings
import java.net.ServerSocket

object ElasticTestClient {

  def getAvailablePort(): Int = {
    val dynamicServerPort = new ServerSocket(0)
    dynamicServerPort.setReuseAddress(true)
    val port = dynamicServerPort.getLocalPort
    dynamicServerPort.close()
    port
  }

  val host = "localhost"
  val clusterName = "example-test-cluster"
  val port = getAvailablePort()

  private val tempDir = Files.createTempDir()
  private val settings = Settings
    .builder()
    .put("cluster.name", "example-test-cluster")
    .put("path.home", s"${tempDir.getAbsolutePath}/elasticsearch-home")
    .put("path.data", s"${tempDir.getAbsolutePath}/elasticsearch-data")
    .put("path.repo", s"${tempDir.getAbsolutePath}/elasticsearch-repo")
    .put("http.port", port)
    .build()

  System.setProperty("es.set.netty.runtime.available.processors", "false")
  val node = LocalNode(settings)

  println("Starting elasticsearch")
  val client: Client = node.client()
  val elastic4sClient = ElasticClient(ElasticProperties(s"http://$host:$port"))
}
