package com.quantexa.example.taskloading.rest

import java.util.concurrent.TimeUnit

import cats.effect.{IO, Resource, Timer}
import com.quantexa.acl.AclAPI.PrivilegeSetUpdatedResponse
import com.quantexa.example.taskloading.circe.Decoders._
import com.quantexa.example.taskloading.model.Model._
import com.quantexa.example.taskloading.jackson.Jackson
import com.quantexa.explorer.tasks.api.Model.{TaskListDefinitionId, _}
import com.quantexa.explorer.tasks.api.TaskAPI._
import com.quantexa.explorer.tasks.loader.api.TaskLoaderAPI.{CleanUpResponse, QueueStatusResponse}
import com.quantexa.explorer.tasks.rest.{CreateTaskListBody, CreateTaskListDefinitionBody, UpdateTaskListDefinitionPrivilegeSetBody, _}
import com.quantexa.graph.script.utils.AuthToken
import com.quantexa.graph.script.utils.RestScalaClient.{login, timeout}
import com.quantexa.queue.api.Model.WorkId
import io.circe.Json
import io.circe.generic.auto.exportDecoder
import org.http4s.Uri
import org.http4s.client.Client

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Code which directly makes requests to product end points
  */
object Requests {

  implicit private val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  private def stringToUri(address: String): IO[Uri] = IO.fromEither(Uri.fromString(address))

  type HTTPClient = Resource[IO, Client[IO]]

  def getLoginToken(loginUrl: String, username: String, password: String, loginTimeout: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS))(implicit httpClient: HTTPClient): IO[AuthToken] = {
    for {
      loginUri <- stringToUri(loginUrl)
      token <- timeout(login(loginUri, username, password, httpClient), loginTimeout, s"Login")
    } yield token
  }

  def cleanUp(cleanUpRequestBody: CleanUpRequestBody, loginUrl: String, explorerUrl: String, username: String, password: String)(implicit httpClient: HTTPClient): IO[CleanUpResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + "/task-loader/clean-up")
      token <- getLoginToken(loginUrl, username, password)
      result <- JacksonRestScalaClient.post[CleanUpResponse](cleanUpRequestBody, explorerUri, httpClient, token)
    } yield result
  }

  def getQueueStatus(queueStatusRequestBody: QueueStatusRequestBody, loginUrl: String, explorerUrl: String, username: String, password: String)(implicit httpClient: HTTPClient): IO[QueueStatusResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + "/task-loader/queue-status")
      token <- getLoginToken(loginUrl, username, password)
      result <- JacksonRestScalaClient.post[QueueStatusResponse](queueStatusRequestBody, explorerUri, httpClient, token)
    } yield result
  }

  def loadTaskType(createTaskTypeBody: CreateTaskTypeBody, loginUrl: String, explorerUrl: String, username: String, password: String)(implicit httpClient: HTTPClient): IO[CreateTaskTypeResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + "/tasktype")
      token <- getLoginToken(loginUrl, username, password)
      result <- JacksonRestScalaClient.post[CreateTaskTypeResponse](createTaskTypeBody, explorerUri, httpClient, token)
    } yield result
  }

  def loadTaskListDefinition(createTaskListDefinitionBody: CreateTaskListDefinitionBody, loginUrl: String, explorerUrl: String, username: String, password: String)(implicit httpClient: HTTPClient): IO[CreateTaskListDefinitionResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + s"/task-list-definition")
      token <- getLoginToken(loginUrl, username, password)
      result <- JacksonRestScalaClient.put[CreateTaskListDefinitionResponse](createTaskListDefinitionBody, explorerUri, httpClient, token)
    } yield result
  }

  def loadTaskListDefinitionPrivilege(updateTaskListDefinitionPrivilegeSetBody: UpdateTaskListDefinitionPrivilegeSetBody, taskListDefinitionId: String, loginUrl: String, explorerUrl: String, username: String, password: String)(implicit httpClient: HTTPClient): IO[PrivilegeSetUpdatedResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + s"/task-list-definition/$taskListDefinitionId/privilege-set")
      token <- getLoginToken(loginUrl, username, password)
      result <- JacksonRestScalaClient.put[PrivilegeSetUpdatedResponse](updateTaskListDefinitionPrivilegeSetBody, explorerUri, httpClient, token)
    } yield result
  }

  def loadTaskList(createTaskListBody: CreateTaskListBody, loginUrl: String, explorerUrl: String, username: String, password: String)(implicit httpClient: HTTPClient): IO[CreateTaskListResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + s"/tasklist")
      token <- getLoginToken(loginUrl, username, password)
      result <- JacksonRestScalaClient.post[CreateTaskListResponse](createTaskListBody, explorerUri, httpClient, token, 5)
    } yield result
  }

  def loadTask(createTaskNewInvestigationBody: CreateTaskNewInvestigationBody, loginUrl: String, explorerUrl: String, username: String, password: String)(implicit httpClient: HTTPClient): IO[WorkId] = {
    for {
      explorerUri <- stringToUri(explorerUrl + "/task")
      token <- getLoginToken(loginUrl, username, password)
      result <- JacksonRestScalaClient.post[WorkId](createTaskNewInvestigationBody, explorerUri, httpClient, token)
    } yield result
  }

  def getTaskLists(loginUrl: String, explorerUrl: String, username: String, password: String, page: Int, pageSize: Int)(implicit httpClient: HTTPClient): IO[GetTaskListsResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + s"/tasklists?page=${page}&pageSize=${pageSize}")
      token <- getLoginToken(loginUrl, username, password)
      result <- JacksonRestScalaClient.get[Json](None, explorerUri, httpClient, token, 5)
      resultDecoded = Jackson.objectMapper.readValue(result.toString(), classOf[GetTaskListsResponse])
    } yield resultDecoded
  }

  def getTasksFromTaskList[E, O](taskListId: TaskListId, loginUrl: String, explorerUrl: String, username: String, password: String, page: Int, pageSize: Int)(implicit httpClient: HTTPClient): IO[GetRedactedTaskListResponseThin[E, O]] = {
    for {
      explorerUri <- stringToUri(explorerUrl + s"/tasklist/${taskListId.value}?page=${page}&pageSize=${pageSize}")
      token <- getLoginToken(loginUrl, username, password)
      result <- JacksonRestScalaClient.get[Json](None, explorerUri, httpClient, token, 5)
      resultDecoded = Jackson.objectMapper.readValue(result.toString(), classOf[GetRedactedTaskListResponseThin[E, O]])
    } yield resultDecoded
  }

  def getPrivilegeSets(taskListDefinitionId: TaskListDefinitionId, loginUrl: String, explorerUrl: String, username: String, password: String)(implicit httpClient: HTTPClient): IO[GetTaskListDefinitionPrivilegeSetsResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + s"/task-list-definition/${taskListDefinitionId.value}/privilege-sets")
      token <- getLoginToken(loginUrl, username, password)
      result <- JacksonRestScalaClient.get[GetTaskListDefinitionPrivilegeSetsResponse](None, explorerUri, httpClient, token, 5)
    } yield result
  }

}