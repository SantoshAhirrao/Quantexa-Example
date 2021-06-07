package com.quantexa.example.graph.scripting.api

import com.fasterxml.jackson.databind.JsonNode
import com.quantexa.explorer.tasks.api.Model.{ScoreSet, TaskListId}
import com.quantexa.explorer.tasks.api.TaskAPI.TaskDataSchema
import com.quantexa.investigation.api.model.InvestigationId
import com.quantexa.resolver.core.EntityGraph.DocumentId
import com.quantexa.security.api.SecurityModel.SecurityContext

object GraphScriptTaskAPI {

  case class TaskTypeSubstep(taskTypeName: String)

  case class TaskListDefinitionSubstep(taskListDefinitionName: String,
                                       scoreSet: ScoreSet,
                                       extraDataSchema: TaskDataSchema,
                                       outcomeSchema: TaskDataSchema)

  case class TaskListSubstep(taskListName: String)

  case class TaskStep(name: String,
                      subject: DocumentId,
                      assignee: String,
                      investigationId: InvestigationId,
                      extraData: Option[Map[String, JsonNode]])

  case class TaskStageInput(taskTypeSubstep: TaskTypeSubstep,
                            taskListDefinitionSubstep: TaskListDefinitionSubstep,
                            taskListSubstep: TaskListSubstep,
                            singleTaskList: Option[TaskListId],
                            taskStep: TaskStep)


  object TaskStageInput {
    /* Helper method in creating a TaskStageInput exposing only fields passed in just before a TaskStage.
     * TaskType, scoreSet and assignee are constants throughout. */
    def defaultTaskInput(taskListDefinitionName: String,
                         taskListName: String,
                         singleTaskList: Option[TaskListId],
                         documentId: DocumentId,
                         investigationId: InvestigationId): TaskStageInput = {
      TaskStageInput(
        taskTypeSubstep = TaskTypeSubstep(taskTypeName = "riskyAddress"),
        taskListDefinitionSubstep = TaskListDefinitionSubstep(
          taskListDefinitionName = taskListDefinitionName,
          scoreSet = ScoreSet("fiu-smoke"),
          extraDataSchema = new TaskDataSchema(Map.empty),
          outcomeSchema = new TaskDataSchema(Map.empty)),
        taskListSubstep = TaskListSubstep(taskListName = taskListName),
        singleTaskList = singleTaskList,
        taskStep = TaskStep(
          name = s"Task for $documentId",
          subject = documentId,
          assignee = "aaron",
          investigationId = investigationId,
          extraData = None)
      )
    }
  }

}
