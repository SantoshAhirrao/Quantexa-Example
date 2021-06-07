package com.quantexa.example.taskloading.model

import com.quantexa.explorer.tasks.api.TaskAPI
import com.quantexa.explorer.tasks.api.TaskAPI.{TaskDataField, TaskDataSchema}

/**
  * Case classes that projects are more likely to want to modify (e.g. Extra Data)
  */
object ProjectModel {

  /**
    * Contains all the information relating to a score that a project might want to display on the task page
    * Things like related transaction ids could be added here
    */
  case class TaskScoreInfo(
                            subjectId: String,
                            subjectTotalScore: Double,
                            scoreId: String,
                            group: String,
                            weightedSeverity: Double,
                            scoreDescription: String,
                            contributing: Boolean)

  case class ScoreInformation(candidateContribution: Option[Double],
                              contribution: Double,
                              contributing: Boolean)

  case class ScorecardOutputInformation(scorecardScore: Double,
                                        scoreInformation: scala.collection.Map[String, ScoreInformation])

  /**
    * Used for selecting tasks to raise
    */
  case class TaskInformationWorking(
                                     customerId: String,
                                     date: java.sql.Date,
                                     scorecardOutput: ScorecardOutputInformation,
                                     rolesAbleToSeeTask: Seq[String],
                                     taskListId: Option[String],
                                     taskListName: Option[String],
                                     underConsideration: Boolean // False only if been selected to raise
                                   ) {
    def toTaskInformationFinal(taskListId: String, taskListName: String) =
      TaskInformationFinal(
        customerId = this.customerId,
        date = this.date,
        scorecardOutput = this.scorecardOutput,
        rolesAbleToSeeTask = this.rolesAbleToSeeTask,
        taskListId = taskListId,
        taskListName = taskListName
      )
  }

  /**
    * Used for the consolidated view of tasks raised; feeds into selection of tasks to raise
    */
  case class TaskInformationFinal(customerId: String,
                                  date: java.sql.Date,
                                  scorecardOutput: ScorecardOutputInformation,
                                  rolesAbleToSeeTask: Seq[String],
                                  taskListId: String,
                                  taskListName: String) {
    def toTaskInformationWorking() = TaskInformationWorking(
      customerId = this.customerId,
      date = this.date,
      scorecardOutput = this.scorecardOutput,
      rolesAbleToSeeTask = this.rolesAbleToSeeTask,
      taskListId = Some(taskListId),
      taskListName = Some(taskListName),
      underConsideration = false
    )
  }

  case class TaskLoadSuccess(taskName: String,
                             taskListName: String,
                             taskListId: String)

  /**
    * The extra data that will be loaded for each task
    */
  case class ExtraData(subjectId: String,
                       subjectDocumentType: String,
                       name: Option[String],
                       finalScore: Int,
                       taskScoreInfo: Seq[TaskScoreInfo],
                       rolesAbleToSeeTask: Seq[String])

  /**
    * The outcome data model
    */
  case class OutcomeData(status: String, comments: String)

  val extraDataSchema = new TaskDataSchema(Map(
    "subjectId" -> new TaskDataField(false, TaskAPI.StringType),
    "subjectDocumentType" -> new TaskDataField(false, TaskAPI.StringType),
    "name" -> new TaskDataField(false, TaskAPI.StringType),
    "finalScore" -> new TaskDataField(false, TaskAPI.NumberType),
    "taskScoreInfo" -> new TaskDataField(false, TaskAPI.ComplexType),
    "rolesAbleToSeeTask" -> new TaskDataField(false, TaskAPI.ComplexType),
    "isInstitution" -> new TaskDataField(false, TaskAPI.BooleanType)))

  val outcomeDataSchema = new TaskDataSchema(Map(
    "status" -> new TaskDataField(false, TaskAPI.StringType),
    "comments" -> new TaskDataField(false, TaskAPI.StringType)
  ))

}
