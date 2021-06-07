package com.quantexa.example.etl.projects.fiu.rest

import com.quantexa.engSpark.model.ResolverModel.Document
import com.quantexa.graph.script.utils.RestScalaClient.post
import com.quantexa.example.etl.projects.fiu.rest.RestClientUtils._
import io.circe.Json
import io.circe.generic.auto._
import cats.effect.{IO, Timer}
import com.quantexa.acl.AclAPI.PrivilegeSetUpdatedResponse
import com.quantexa.explorer.tasks.api.Model.TaskListId
import com.quantexa.explorer.tasks.api.TaskAPI.{CreateTaskListDefinitionResponse, CreateTaskListResponse, CreateTaskTypeResponse}
import com.quantexa.graph.script.utils.RestScalaClient
import com.quantexa.queue.api.Model.WorkId
import org.apache.spark.sql.{Encoder, Encoders}

import scala.concurrent.ExecutionContext

object TaskRestClient {
  /***
    * Model holding information to be posted to the task REST API to create a Task Type.
    *
    * @param name The name of the task type
    * @param description The optional description of the task type
    * @param scoreSet The score set to be applied for the task type
    */
  case class CreateTaskTypeRequest(
                                    name: String
                                    , description: Option[String]
                                    , scoreSet: String
                                  )

  /***
    * Model holding information to be posted to the task REST API to create a Task.
    *
    * @param name            The name of the Task
    * @param listId          The ID of the associated Task List
    * @param investigationId The resolver request associated to the Task
    * @param subject         The subject of the Task
    * @param assignee        The assignee of the task
    * @param extraData       An optional field of any type which will be posted alongside your task and available in explorer
    * @tparam T
    */
  case class CreateTaskRequest[T](
                                   name: String
                                   , listId: String
                                   , investigationId: TaskListId
                                   , subject: Document
                                   , assignee: String
                                   , extraData: Option[T]
                                 )

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val taskResponseEncoder: Encoder[Either[Throwable, WorkId]] = Encoders.kryo[Either[Throwable, WorkId]]

  /**
    * Sends a request to the UI to create a new task type
    *
    * @param data         Json containing information about the task type
    * @param loginUrl     Url for the authentication access point
    * @param explorerUrl  Url for the explorer access point
    * @param username     Username to generate authentication token
    * @param password     Password to generate authentication token
    * @return
    * An IO of the response from the server for creating a task type
    */
  def loadTaskType(data: Json, loginUrl: String, explorerUrl: String, username: String, password: String): IO[CreateTaskTypeResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + "/tasktype")
      token <- getLoginToken(loginUrl, username, password)
      result <- post[CreateTaskTypeResponse](data,explorerUri,RestScalaClient.defaultHttpClient,token)
    } yield result
  }

  /**
    * Sends a request to the UI to create a new task list definition
    *
    * @param data         Json containing information about the task list definition
    * @param loginUrl     Url for the authentication access point
    * @param explorerUrl  Url for the explorer access point
    * @param username     Username to generate authentication token
    * @param password     Password to generate authentication token
    * @return
    * An IO of the response from the server for creating a task list definition
    */
  def loadTaskListDefinition(data: Json, loginUrl: String, explorerUrl: String, username: String, password: String): IO[CreateTaskListDefinitionResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + s"/task-list-definition")
      token <- getLoginToken(loginUrl, username, password)
      result <- put[CreateTaskListDefinitionResponse](data,explorerUri,RestScalaClient.defaultHttpClient,token)
    } yield result
  }

  /**
    * Sends a request to the UI to add privileges to a task list definition
    *
    * @param data                 Json containing information about the task definition privileges
    * @param taskListDefinitionId ID of the task list definition the privileges are being added to
    * @param loginUrl             Url for the authentication access point
    * @param explorerUrl          Url for the explorer access point
    * @param username             Username to generate authentication token
    * @param password             Password to generate authentication token
    * @return
    * An IO of the response from the server for adding privileges to a task list definition
    */
  def loadTaskListDefinitionPrivilege(data: Json, taskListDefinitionId: String, loginUrl: String, explorerUrl: String, username: String, password: String): IO[PrivilegeSetUpdatedResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + s"/task-list-definition/$taskListDefinitionId/privilege-set")
      token <- getLoginToken(loginUrl, username, password)
      result <- put[PrivilegeSetUpdatedResponse](data,explorerUri,RestScalaClient.defaultHttpClient,token)
    } yield result
  }

  /**
    * Sends a request to the UI to create a new task list
    *
    * @param data         Json containing information about the task list
    * @param loginUrl     Url for the authentication access point
    * @param explorerUrl  Url for the explorer access point
    * @param username     Username to generate authentication token
    * @param password     Password to generate authentication token
    * @return
    * An IO of the response from the server for creating a task list
    */
  def loadTaskList(data: Json, loginUrl: String, explorerUrl: String, username: String, password: String): IO[CreateTaskListResponse] = {
    for {
      explorerUri <- stringToUri(explorerUrl + s"/tasklist")
      token <- getLoginToken(loginUrl, username, password)
      result <- post[CreateTaskListResponse](data,explorerUri,RestScalaClient.defaultHttpClient,token, 5)
    } yield result
  }

  /**
    * Sends a request to the UI to add a task to a task list
    *
    * @param data         Json containing information about the task
    * @param loginUrl     Url for the authentication access point
    * @param explorerUrl  Url for the explorer access point
    * @param username     Username to generate authentication token
    * @param password     Password to generate authentication token
    * @return
    * An IO of the response from the server for adding a task to a task list
    */
  def loadTask(data: Json, loginUrl: String, explorerUrl: String, username: String, password: String): IO[WorkId] = {
    for {
      explorerUri <- stringToUri(explorerUrl + "/task/investigation")
      token <- getLoginToken(loginUrl, username, password)
      result <- post[WorkId](data,explorerUri,RestScalaClient.defaultHttpClient,token)
    } yield result
  }
}
