package com.quantexa.example.taskloading.model

import com.quantexa.acl.AclModel
import com.quantexa.acl.AclModel.{PrivilegeSetId, PrivilegeSetName}
import com.quantexa.explorer.tasks.api.Model._
import com.quantexa.explorer.tasks.rest.CreateTaskNewInvestigationBody
import com.quantexa.investigation.api.model.InvestigationId
import com.quantexa.resolver.core.EntityGraph.DocumentId
import com.quantexa.scoring.framework.scorecarding.ScoringModels.{ScoreName, WeightedScore}

/**
  * Case classes that projects are less likely to want to modify
  */
object Model {

  /**
    * Mirrors WeightedStandardisedScoreOutput in Quantexa Explorer, but with collection.Map to make it Spark friendly
    * WeightedStandardisedScoreOutput will likely be corrected in the future so this will be unnecessary
    */
  case class CustomWeightedStandardisedScoreOutput(subjectId: Option[String],
                                                   scorecardScore: Double,
                                                   weightedScoreOutputMap: scala.collection.Map[ScoreName, WeightedScore])

  case class TaskInfo(taskName: String, taskId: TaskId, investigationId: InvestigationId)

  case class ExistingTaskListInformation(taskListId: TaskListId,
                                         taskListDefinitionId: TaskListDefinitionId,
                                         taskInfo: Seq[TaskInfo],
                                         privilegeSets: Set[AclModel.PrivilegeSet])

  /**
    * Typed version of the important bits of RedactedTaskInfoView (score discarded for example)
    * O - OutcomeData
    * E - ExtraData
    */
  case class RedactedTaskInfoViewThin[O, E](
                                               id: TaskId,
                                               name: String,
                                               subject: DocumentId,
                                               assignee: Option[String],
                                               outcome: O,
                                               investigation: InvestigationId,
                                               extraData: E,
                                               metadata: TaskMetadata
                                             )

  /**
    * Version of GetRedactedTaskListResponse that uses RedactedTaskInfoViewThin
    * O - OutcomeData
    * E - ExtraData
    */
  case class GetRedactedTaskListResponseThin[O, E](taskList: TaskListInfoView, tasks: List[RedactedTaskInfoViewThin[O, E]], totalSize: Int)

  case class TaskListDefinitionInfo(
                                     taskTypeName: String,
                                     taskTypeId: TaskTypeId,
                                     taskListDefinitionName: String,
                                     taskListDefinitionId: TaskListDefinitionId,
                                     privilegeSets: collection.Map[PrivilegeSetName, PrivilegeSetId]
                                   )

  case class TaskNameToTaskRequest(taskName: String, createTaskNewInvestigationBody: CreateTaskNewInvestigationBody)

  case class TaskNameToTaskWorkId(taskName: String, taskWorkId: String)

  /**
    * Helper class that is used for creating task related Privilege Sets
    * Privilege sets are tied to a TaskListDefinition
    */
  case class TaskPrivilegeSetCreation(taskPrivilegeSetName: String,
                                      privilegeSetFunction: (TaskListDefinitionId) => Set[AclModel.Privilege])


}