package com.quantexa.example.graph.scripting.rest.scripts

import com.quantexa.example.graph.scripting.rest.api.FullScriptEndpoint
import com.quantexa.example.graph.scripting.stages._
import java.time.LocalDateTime

import com.quantexa.example.graph.scripting.api.GraphScriptTaskAPI.TaskStageInput
import com.quantexa.explorer.tasks.api.Model.TaskListId
import com.quantexa.graph.script.client.RestClientFactory
import com.quantexa.queue.api.Model.WorkId

import scala.concurrent.{ExecutionContext, Future}

/* Do not need so many clients, e.g. TaskClient but put in for completeness for now */
class ExpandScoreAndLoadScript(clientFactory: RestClientFactory) {

  def apply(request: FullScriptEndpoint)(implicit ec: ExecutionContext): Future[WorkId] = {

    val docId = request.documentId
    val taskListId = request.taskListId.map(TaskListId(_))
    val Threshold = request.threshold.getOrElse(100F)

    /* Must import cats implicits to allow a for comprehension computation */
    import cats.implicits._

    val taskListDefinitionName = "taskListDefinitionName"
    val taskListName = s"${LocalDateTime.now}"

    val result = for {
      oneHopOutput <- CustomerOneHopExpansionStage(docId, None, clientFactory)
      scoreGraphOutput <- ScoreCustomerOneHopNetworkStage(oneHopOutput, clientFactory)
      networkAboveThreshold <- FilterHighScoresStage(scoreGraphOutput, Threshold)
      investigation <- LoadInvestigationStage.apply(docId, networkAboveThreshold.entityGraph, clientFactory)
      taskStageInput = TaskStageInput.defaultTaskInput(
        taskListDefinitionName = taskListDefinitionName,
        taskListName = taskListName,
        singleTaskList = taskListId,
        documentId = docId,
        investigationId = investigation.id
      )
      res <- LoadTaskStage(taskStageInput, clientFactory)
    } yield res

    result.value.value.flatMap {
      case Right(workId) => Future.successful(workId)
      case Left(msg) =>  Future.failed(throw new IllegalStateException(msg))
    }
  }
}
