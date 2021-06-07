package com.quantexa.example.graph.scripting.rest.api

import com.quantexa.resolver.core.EntityGraph.DocumentId

case class FullScriptEndpoint(
                   taskListId: Option[String],
                   threshold: Option[Float],
                   documentId: DocumentId
                   )