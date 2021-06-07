package com.quantexa.example.graph.scripting.stages

import cats.effect.{IO, Resource}
import cats.implicits._
import com.quantexa.acl.AclModel.{PrivilegeSetId, PrivilegeSetName, SubjectPrivilegeSetMapping}
import com.quantexa.graph.script.client.RestClientFactory
import com.quantexa.example.graph.scripting.api.GraphScriptTaskAPI._
import com.quantexa.explorer.tasks.api.Model.{TaskListDefinitionId, TaskListId, TaskTypeId}
import com.quantexa.explorer.tasks.api.TaskAPI._
import com.quantexa.explorer.tasks.coordinator.api.TaskCoordinatorAPI.CreateTaskExistingInvestigation
import com.quantexa.explorer.tasks.proto.api.RestModel._
import com.quantexa.explorer.tasks.proto.rest.client.TaskProtoRestClient
import com.quantexa.graph.script.clientrequests.FutureErrorOrWithMetrics
import com.quantexa.graph.script.clientrequests.metrics.TaskResponseWithMetrics
import com.quantexa.graph.script.utils.TaskUtils.allPrivilegesForDefinition
import com.quantexa.queue.api.Model.WorkId
import com.quantexa.security.api.SecurityModel.{RoleId, UserId}

import scala.concurrent.{ExecutionContext, Future}

object LoadTaskStage {

  val stageName = "LoadTaskStage"
  val ALL_PRIVILEGES_NAME = "All-Privileges"
  val USER_TASK_LIST_PRIVILEGES_NAME = "User-Task-List-Privileges"
  val SYSTEM_USER = "SUPER-ADMIN" // The all powerful system/admin user. Could also be a role.
  val USER_BASE_ROLE = "ROLE_USER" // Base role - used to allow all users to access task lists

  /**
    * apply method used as this Object is a function (i.e. usage of Object is a singular function call).
    * This stage takes an investigation and creates a TaskList and Task for it.
    */
  def apply(taskStageInput: TaskStageInput, clientFactory: RestClientFactory)
           (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[WorkId] = {

    val taskClient = clientFactory.getTaskResource

    val taskListDefinitionInput = taskStageInput.taskListDefinitionSubstep

    /*
     This is not as efficient as it could be. createTaskListStep checks to see the taskList is already defined, and only
     creates it if it's not already defined. Therefore, when the task list is already defined, the first 3 steps are
     unnecessary. It may be better to split this stage into two -- LoadTaskStageExistingTaskList and
     CreateTaskListAndLoadTaskStage.
      */
    for {
      taskType <- createTaskTypeStep(taskStageInput.taskTypeSubstep, taskClient)
      taskListDefinition <- createTaskListDefinitionStep(taskType.id, taskListDefinitionInput, taskClient)
      //Create privileges
      privilegeResponse <- generateTaskListPrivilegesStep(
        taskListDefinition.id,
        taskListDefinitionInput.extraDataSchema,
        taskListDefinitionInput.outcomeSchema,
        taskClient
      )
      privilegeSetMap = Map(privilegeResponse.updated.name -> privilegeResponse.updated.id)
      _ = Thread.sleep(5000)
      taskListId <- createTaskListStep(taskListDefinition.id, taskStageInput, privilegeSetMap, taskStageInput.taskListSubstep, taskClient)
      _ = Thread.sleep(5000)
      //Since no privileges are passed in, they default to those defined in the task list.
      taskResponse <- createTaskStep(taskListId.id, None, taskStageInput.taskStep, taskClient)
    } yield taskResponse

  }

  def createTaskStep(taskListId: TaskListId,
                     privileges: Option[SubjectPrivilegeSetMapping],
                     taskStepConfiguration: TaskStep,
                     taskClient: Resource[IO, TaskProtoRestClient])
                    (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[WorkId] = {

    val request = CreateTaskExistingInvestigation(name = taskStepConfiguration.name,
      listId = taskListId.value,
      investigationId = taskStepConfiguration.investigationId,
      subject = taskStepConfiguration.subject,
      assignee = taskStepConfiguration.assignee,
      privileges = privileges,
      extraData = taskStepConfiguration.extraData)

    /* Must wrap this in an TaskResponseWithMetrics to allow a for-comprehension */
    TaskResponseWithMetrics.fromValidatedResult(
      taskClient.use { c => c.createTaskExistingInvestigation(CreateTaskExistingInvestigationBody(request, None, Some("MyGraphScript"))) }.unsafeToFuture(),
      stageName
    )
  }

  def generateTaskListPrivilegesStep(definitionId: TaskListDefinitionId,
                                     extraDataSchema: TaskDataSchema,
                                     outputSchema: TaskDataSchema,
                                     taskClient: Resource[IO, TaskProtoRestClient])
                                    (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[UpdateTaskListDefinitionPrivilegeSetResponse] = {

    val privilegeCreationBodies = UpdateTaskListDefinitionPrivilegeSetBody("ALL", allPrivilegesForDefinition(definitionId, extraDataSchema, outputSchema))
    TaskResponseWithMetrics.fromValidatedResult(
      taskClient.use(taskClient => {
        taskClient.createTaskListDefinitionPrivilegeSet(definitionId.value, privilegeCreationBodies)
      }).unsafeToFuture(), stageName
    )
  }

  def createTaskTypeStep(taskTypeSubstep: TaskTypeSubstep,
                         taskClient: Resource[IO, TaskProtoRestClient])
                        (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[CreateTaskTypeResponse] = {

    /* Must wrap this in an TaskResponseWithMetrics to allow a for-comprehension */
    TaskResponseWithMetrics(
      taskClient.use { c => c.createTaskType(CreateTaskTypeBody(taskTypeSubstep.taskTypeName)) }.unsafeToFuture(),
      stageName
    )
  }

  def createTaskListDefinitionStep(taskType: TaskTypeId,
                                   taskListDefinitionSubstep: TaskListDefinitionSubstep,
                                   taskClient: Resource[IO, TaskProtoRestClient])
                                  (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[CreateTaskListDefinitionResponse] = {

    TaskResponseWithMetrics.fromValidatedResult(
      taskClient.use { c =>
        c.createTaskListDefinition(CreateTaskListDefinitionBody(
          taskListDefinitionSubstep.taskListDefinitionName,
          Some("Description"),
          Some(taskType.value),
          taskListDefinitionSubstep.scoreSet.name,
          Some(taskListDefinitionSubstep.extraDataSchema),
          Some(taskListDefinitionSubstep.outcomeSchema),
          None,
          None))
      }.unsafeToFuture(), stageName)
  }

  def createTaskListStep(taskListDefinitionId: TaskListDefinitionId,
                         taskStageInput: TaskStageInput,
                         privilegeSets: Map[PrivilegeSetName, PrivilegeSetId],
                         taskListSubstep: TaskListSubstep,
                         taskClient: Resource[IO, TaskProtoRestClient])
                        (implicit ec: ExecutionContext): FutureErrorOrWithMetrics[CreateTaskListResponse] = {

    val privileges = new SubjectPrivilegeSetMapping(
      Map(new RoleId(USER_BASE_ROLE) -> Set(privilegeSets(PrivilegeSetName(USER_TASK_LIST_PRIVILEGES_NAME)))),
      Map(new UserId(SYSTEM_USER) -> Set(privilegeSets(PrivilegeSetName(ALL_PRIVILEGES_NAME))))
    )

    if (taskStageInput.singleTaskList.isDefined) {
      TaskResponseWithMetrics(
        Future.successful(new CreateTaskListResponse(taskStageInput.singleTaskList.get)), stageName)
    }
    else {
      TaskResponseWithMetrics.fromValidatedResult(
        taskClient.use { c =>
          c.createTaskList(CreateTaskListBody(
            taskListSubstep.taskListName,
            taskListDefinitionId.toString,
            None,
            Some(privileges)))
        }.unsafeToFuture,
        stageName
      )
    }
  }
}
