package com.quantexa.example.graph.scripting

case class GraphScriptBulkLoaderConfig(
                                oneHopStage: Option[StageConfig] = None,
                                url: Option[String],
                                username: String,
                                password: String,
                                sslEnabled: Boolean
                               )