package com.quantexa.example.taskloading.rest

import com.fasterxml.jackson.databind.JsonNode
import com.quantexa.acl.AclModel.{PrivilegeSetId, PrivilegeSetName}
import com.quantexa.example.taskloading.config.Constants._
import com.quantexa.example.taskloading.model.Model.TaskPrivilegeSetCreation
import com.quantexa.example.taskloading.model.ProjectModel
import com.quantexa.example.taskloading.privileges.ProjectPrivileges._
import com.quantexa.example.taskloading.utils.ResolverUtils._
import com.quantexa.explorer.tasks.api.Model.{TaskListDefinitionId, TaskListId}
import com.quantexa.explorer.tasks.rest._
import com.quantexa.resolver.core.EntityGraph.DocumentId
import com.quantexa.resolver.core.EntityGraphLite.LiteGraph

/**
  * This object contains code that generates the bodies of REST requests
  */
object RequestBodyGeneration {

  // Will clean up everything from the task loader
  def createCleanUpBody() = new CleanUpRequestBody(None, None, None, None, None, None)

  // Will return entire queue status
  def createQueueStatusBody() = new QueueStatusRequestBody(None, None, None, None, None, None, None)

  def createTaskTypeBody(name: String) = new CreateTaskTypeBody(name)

  def createTaskListDefinitionBody(taskListDefinitionName: String, taskTypeId: String) = new CreateTaskListDefinitionBody(
    name = taskListDefinitionName,
    description = Some(taskListDefinitionName),
    taskTypeId = Some(taskTypeId),
    scoreSet = TASK_LOADING_SCORE_SET,
    extraDataSchema = Some(ProjectModel.extraDataSchema),
    outcomeSchema = Some(ProjectModel.outcomeDataSchema),
    None,
    None)

  def createUpdateTaskListDefinitionPrivilegeSetBody(taskListDefinitionId: TaskListDefinitionId, taskPrivilegeSetCreation: TaskPrivilegeSetCreation) = new UpdateTaskListDefinitionPrivilegeSetBody(
    name = taskPrivilegeSetCreation.taskPrivilegeSetName,
    privileges = taskPrivilegeSetCreation.privilegeSetFunction(taskListDefinitionId)
  )

  def createTaskListBody(taskListName: String,
                         taskListDefinitionId: String,
                         privilegeSets: collection.Map[PrivilegeSetName, PrivilegeSetId]
                        ) = new CreateTaskListBody(
    name = taskListName,
    taskListDefinitionId = taskListDefinitionId,
    description = Some(taskListName),
    privileges = Some(taskListPrivilegeSetMapping(privilegeSets))
  )

  def createTaskNewInvestigationBody(customerId: String,
                                     customerDocumentType: String,
                                     taskName: String,
                                     taskListId: TaskListId,
                                     liteGraph: Option[LiteGraph],
                                     privilegeSets: collection.Map[PrivilegeSetName, PrivilegeSetId],
                                     allowedRoles: Seq[String],
                                     assignee: String,
                                     extraData: Map[String, JsonNode]): CreateTaskNewInvestigationBody = {
    val customerDocumentId = new DocumentId(customerDocumentType, customerId)
    new CreateTaskNewInvestigationBody(
      name = taskName,
      listId = taskListId.value,
      resolverRequest = createResolverRequest(customerDocumentId, liteGraph),
      subject = customerDocumentId,
      assignee = assignee,
      privileges = Some(taskPrivilegeSetMapping(privilegeSets, allowedRoles)),
      extraData = Some(extraData),
      allowedDocumentTypes = None,
      priority = None,
      groupId = None
    )
  }

}
