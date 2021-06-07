package com.quantexa.example.taskloading.utils

import cats.data.{NonEmptyList, OptionT}
import cats.effect.{IO, Timer}
import cats.implicits._
import com.quantexa.acl.AclAPI.PrivilegeSetUpdatedResponse
import com.quantexa.acl.AclModel.{PrivilegeSetId, PrivilegeSetName}
import com.quantexa.example.taskloading.model.Model._
import com.quantexa.example.taskloading.rest.RequestBodyGeneration._
import com.quantexa.example.taskloading.rest.Requests.{getPrivilegeSets, getQueueStatus, getTaskLists, getTasksFromTaskList, loadTask, loadTaskList, loadTaskListDefinition, loadTaskListDefinitionPrivilege, loadTaskType}
import com.quantexa.example.taskloading.utils.GeneralUtils.repeatLimitedTimes
import com.quantexa.explorer.tasks.api.Model.{TaskListDefinitionId, TaskListId, TaskListInfoView}
import com.quantexa.explorer.tasks.api.TaskAPI
import com.quantexa.explorer.tasks.api.TaskAPI.GetTaskListsResponse
import com.quantexa.explorer.tasks.loader.api.TaskLoaderAPI._
import com.quantexa.queue.api.Model._
import org.apache.log4j.Logger

import com.quantexa.example.taskloading.rest.Requests.HTTPClient

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

/**
  * Lots of helpful methods that wrap base requests
  */
object TaskUtils {

  implicit private val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  private case class PaginationHelper[T](result: Seq[T], pageNumber: Int, continue: Boolean)

  private object PaginationHelper {
    def initial[T] = PaginationHelper(Seq.empty[T], 0, true)
  }

  private def paginateRequest[T, R](pageSize: Int,
                                    getPageFunc: (Int) => (Int) => IO[R],
                                    responseToResultFunc: (R) => Seq[T],
                                    continueFunc: (R, Int) => Boolean): IO[Seq[T]] = {

    val fixedPageSizeGetPageFunc = getPageFunc(pageSize)

    def updatePaginationHelper(paginationHelper: PaginationHelper[T], resp: R): PaginationHelper[T] = {
      val newResults = responseToResultFunc(resp)
      val continue = continueFunc(resp, pageSize)
      PaginationHelper(paginationHelper.result ++ newResults, paginationHelper.pageNumber + 1, continue)
    }

    PaginationHelper.initial[T].iterateWhileM(paginationHelper =>
      fixedPageSizeGetPageFunc(paginationHelper.pageNumber)
        .map(updatePaginationHelper(paginationHelper, _)))(_.continue).map(_.result)
  }

  /**
    * Extract relevant information about every task in a task list
    */
  def getTasksFromTaskListUsingPagination[E, O](taskListId: TaskListId, loginURL: String, explorerURL: String,
                                                username: String, password: String, pageSize: Int)(implicit httpClient: HTTPClient): IO[Seq[TaskInfo]] = {

    def getTasksFromTaskListPageNumber(pageSize: Int)(page: Int): IO[GetRedactedTaskListResponseThin[E, O]] = {
      getTasksFromTaskList[E, O](taskListId, loginURL, explorerURL, username, password, page, pageSize)
    }

    def toResult(r: GetRedactedTaskListResponseThin[E, O]): Seq[TaskInfo] = {
      r.tasks.map(ti => TaskInfo(ti.name, ti.id, ti.investigation))
    }

    def continueFunc(r: GetRedactedTaskListResponseThin[E, O], pageSize: Int): Boolean = {
      r.tasks.size == pageSize
    }

    paginateRequest[TaskInfo, GetRedactedTaskListResponseThin[E, O]](pageSize,
      getTasksFromTaskListPageNumber,
      toResult,
      continueFunc)
  }

  /**
    * Extract relevant information about every task list
    */
  def getTaskListsUsingPagination(loginURL: String, explorerURL: String, username: String,
                                  password: String, pageSize: Int)(implicit httpClient: HTTPClient): IO[Seq[TaskListInfoView]] = {

    def getTaskListsPageNumber(pageSize: Int)(page: Int): IO[GetTaskListsResponse] = {
      getTaskLists(loginURL, explorerURL, username, password, page, pageSize)
    }

    def toResult(r: GetTaskListsResponse): Seq[TaskListInfoView] = {
      r.taskLists
    }

    def continueFunc(r: GetTaskListsResponse, pageSize: Int): Boolean = {
      r.taskLists.size == pageSize
    }

    paginateRequest[TaskListInfoView, GetTaskListsResponse](pageSize,
      getTaskListsPageNumber,
      toResult,
      continueFunc)
  }

  /**
    * Get information about tasks and privileges from an existing task list by name, if the task list exists
    */
  def getTaskListAndTaskListDefinitionByName(taskListName: String, loginURL: String, explorerURL: String,
                                             username: String, password: String,
                                             logger: Logger)(implicit httpClient: HTTPClient): OptionT[IO, (TaskListId, TaskListDefinitionId)] = {
    for {
      taskListsResponse <- OptionT.liftF(getTaskListsUsingPagination(loginURL, explorerURL, username, password, 100))
      (taskListId, taskListDefinitionId) <- OptionT.fromOption[IO](getTaskListIdAndDefinitionOfInterest(taskListsResponse, taskListName, logger))
    } yield (taskListId, taskListDefinitionId)
  }

  /**
    * Check if a task list with a given name exists
    */
  def checkIfTaskListExistsByName(taskListName: String, loginURL: String, explorerURL: String,
                                  username: String, password: String, logger: Logger)(implicit httpClient: HTTPClient): IO[Boolean] = {
    getTaskListAndTaskListDefinitionByName(taskListName: String, loginURL, explorerURL,
      username, password, logger: Logger).isDefined
  }

  /**
    * Get information about tasks and privileges from an existing task list by name
    */
  def getInformationFromExistingTaskList[E, O](taskListName: String, loginURL: String, explorerURL: String,
                                               securityURL: String, username: String,
                                               password: String, logger: Logger)(implicit httpClient: HTTPClient): IO[ExistingTaskListInformation] = {
    val existingTaskListInformationOption = getInformationFromTaskListIfExists[E, O](
      taskListName, loginURL, explorerURL, securityURL, username, password, logger).value
    existingTaskListInformationOption.map(info => {
      if (info.isEmpty) throw new IllegalStateException(
        s"${taskListName} does not exist. Create this task list before running this job again.")
      info.get
    })
  }

  /**
    * Check if a task list exists by name, and extract information about the tasks and privileges if it does
    */
  def getInformationFromTaskListIfExists[E, O](taskListName: String, loginURL: String, explorerURL: String,
                                               securityURL: String, username: String, password: String,
                                               logger: Logger)(implicit httpClient: HTTPClient): OptionT[IO, ExistingTaskListInformation] = {

    val existingTaskListInformation = for {
      (taskListId, taskListDefinitionId) <- getTaskListAndTaskListDefinitionByName(taskListName, loginURL, explorerURL, username, password, logger)
      taskInfo <- OptionT.liftF(getTasksFromTaskListUsingPagination[E, O](taskListId, loginURL, explorerURL, username, password, 100))
      privileges <- OptionT.liftF(getPrivilegeSets(taskListDefinitionId, loginURL, explorerURL, username, password).map(_.privilegeSets))
      response = ExistingTaskListInformation(
        taskListId = taskListId,
        taskListDefinitionId = taskListDefinitionId,
        taskInfo = taskInfo,
        privilegeSets = privileges)
    } yield response

    existingTaskListInformation
  }

  private def getTaskListIdAndDefinitionOfInterest(taskLists: Seq[TaskListInfoView], taskListName: String,
                                                   logger: Logger): Option[(TaskListId, TaskListDefinitionId)] = {
    val taskListsMatching = taskLists.filter(t => t.name.toUpperCase == taskListName.toUpperCase)
    taskListsMatching.size match {
      case 0 => None
      case 1 => taskListsMatching.headOption.map(tl => (tl.id, tl.taskListDefinition))
      case _ => throw new IllegalStateException(s"More than one task list with name: ${taskListName} found")
    }
  }

  /**
    * Polls the work queue until there is no work remaining (or max retries is reached)
    * Returns a map of WorkId -> Final Status
    * Useful for blocking until task loading is complete
    */
  def getTaskLoadingOutcomes(loginUrl: String, explorerUrl: String, username: String, password: String, logger: Logger, pollFrequency: FiniteDuration, maxTimesToPoll: Int)(implicit httpClient: HTTPClient): IO[Map[String, String]] = {
    val queueStatus = getQueueStatus(createQueueStatusBody(), loginUrl, explorerUrl, username, password)
    val queueStatusLogging = queueStatus.map(status => {
      val statusCounts = status.status.items.toSeq.map(_.status.name).groupBy(identity).mapValues(_.size)
      val statusCountsFormatted = statusCounts.map { case (status, count) => s"${status}: ${count}" }.mkString(" ; ")
      logger.info(statusCountsFormatted)
      status
    })

    val queueStatusBreak = (qs: QueueStatusResponse) => {
      val notYetFinishedStatuses: Set[Status] = Set(Status.Processing, Status.Queued)
      val notYetFinishedWorkInQueue = qs.status.items.exists(item => notYetFinishedStatuses.contains(item.status))
      !notYetFinishedWorkInQueue //Break if there is not unfinished work
    }

    val successFailures = repeatLimitedTimes(queueStatusLogging, queueStatusBreak, pollFrequency, maxTimesToPoll)
    val workIdToStatus = successFailures.map(_.status.items.map(item => (item.workId.value, item.status.name)).toMap)
    workIdToStatus
  }


  /**
    * Load a task type and definition, and assign privilege sets to the definition
    */
  def createTaskTypeTaskDefinitionAndPrivileges(taskTypeName: String,
                                                taskListDefinitionName: String,
                                                privilegeSetsToCreate: NonEmptyList[TaskPrivilegeSetCreation],
                                                loginURL: String,
                                                explorerURL: String,
                                                username: String,
                                                password: String,
                                                logger: Logger)(implicit httpClient: HTTPClient): IO[TaskListDefinitionInfo] = {

    implicit val contextShift = cats.effect.IO.contextShift(global)

    def createTaskPrivilegeSetInformationMap(responses: NonEmptyList[PrivilegeSetUpdatedResponse]): Map[PrivilegeSetName, PrivilegeSetId] = {
      responses.map(resp => (resp.updated.name, resp.updated.id)).toList.toMap
    }

    for {

      //Create task type
      taskTypeResponse <- loadTaskType(createTaskTypeBody(taskTypeName), loginURL, explorerURL, username, password)
      _ = logger.info(s"Task type successfully loaded with response: ${taskTypeResponse.id.value}")

      //Create task list definition
      taskTypeDefinitionBody = createTaskListDefinitionBody(taskListDefinitionName, taskTypeResponse.id.value)
      taskListDefinitionResponse <- loadTaskListDefinition(taskTypeDefinitionBody, loginURL, explorerURL, username, password)
      _ = logger.info(s"Task list definition successfully loaded with response: ${taskListDefinitionResponse.id.value}")

      //Create privileges
      privilegeCreationBodies = privilegeSetsToCreate.map(p => createUpdateTaskListDefinitionPrivilegeSetBody(taskListDefinitionResponse.id, p))
      privilegesCreationResponse <- privilegeCreationBodies.map {
        loadTaskListDefinitionPrivilege(_, taskListDefinitionResponse.id.value, loginURL, explorerURL, username, password)
      }.parSequence
      _ = logger.info(s"Privileges successfully loaded")

      privilegeSetMap = createTaskPrivilegeSetInformationMap(privilegesCreationResponse)

      response = TaskListDefinitionInfo(
        taskTypeName = taskTypeName,
        taskTypeId = taskTypeResponse.id,
        taskListDefinitionName = taskListDefinitionName,
        taskListDefinitionId = taskListDefinitionResponse.id,
        privilegeSets = privilegeSetMap)

    } yield response
  }


  /**
    * Create a new task list and assign privileges
    */
  def createNewTaskListCheckingForExisting(taskListName: String, taskListDefinitionInfo: TaskListDefinitionInfo,
                                           loginURL: String, explorerURL: String, username: String,
                                           password: String, logger: Logger)(implicit httpClient: HTTPClient): IO[TaskAPI.CreateTaskListResponse] = {
    for {
      taskListExists <- TaskUtils.checkIfTaskListExistsByName(taskListName, loginURL, explorerURL, username, password, logger)
      taskListNameToCreate = if (!taskListExists) taskListName else throw new IllegalStateException(s"Task list with name: ${taskListName} already exists")
      _ = logger.info(s"Creating new task list: $taskListName")
      taskListResponse <- createNewTaskList(taskListNameToCreate, taskListDefinitionInfo, loginURL, explorerURL, username, password, logger)
    } yield taskListResponse
  }

  private def createNewTaskList(taskListName: String, taskListDefinitionInfo: TaskListDefinitionInfo,
                                loginURL: String, explorerURL: String, username: String,
                                password: String, logger: Logger)(implicit httpClient: HTTPClient): IO[TaskAPI.CreateTaskListResponse] = {
    val taskListBody = createTaskListBody(taskListName, taskListDefinitionInfo.taskListDefinitionId.value,
      taskListDefinitionInfo.privilegeSets)
    loadTaskList(taskListBody, loginURL, explorerURL, username, password)
  }

}
