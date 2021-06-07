package com.quantexa.example.taskloading.batch.utils

import com.quantexa.example.taskloading.model.Model.{CustomWeightedStandardisedScoreOutput, TaskNameToTaskRequest, TaskNameToTaskWorkId}
import com.quantexa.example.taskloading.model.ProjectModel.TaskScoreInfo
import com.quantexa.example.taskloading.rest.Requests.{HTTPClient, loadTask}
import org.apache.spark.sql.Dataset

object TaskSparkUtils {

  /**
    * Create TaskScoreInfo from scorecard input
    * Could easily be extended to add transactions from underlying
    */
  def getTaskScoreInfoFromScorecard(scorecardOutput: Dataset[CustomWeightedStandardisedScoreOutput]): Dataset[TaskScoreInfo] = {
    val ss = scorecardOutput.sparkSession
    import ss.implicits._

    val taskScoringInformation = scorecardOutput.flatMap(so => {
      val subjectId = so.subjectId.get // Should always be defined
      val totalScore = so.scorecardScore
      val subjectTaskScoreInfo = so.weightedScoreOutputMap.toSeq.map { case (scoreId, weightedScore) => {
        TaskScoreInfo(
          subjectId = subjectId,
          subjectTotalScore = totalScore,
          scoreId = scoreId,
          group = weightedScore.scorecardParameters.group.getOrElse("No Group"),
          weightedSeverity = weightedScore.candidateContribution.get, // Should always be defined
          scoreDescription = weightedScore.description.getOrElse(""),
          contributing = weightedScore.contributesToScorecardScore
        )
      }
      }
      subjectTaskScoreInfo
    })

    taskScoringInformation
  }

  /**
    * Post all tasks in dataset to task loading queue
    */
  def addTasksToLoaderQueue(tasks: Dataset[TaskNameToTaskRequest], loginURL: String, explorerURL: String,
                            username: String, password: String, httpClient: => HTTPClient): Dataset[TaskNameToTaskWorkId] = {
    import tasks.sparkSession.implicits._
    tasks.map(taskNameToTaskRequest => {
      val taskName = taskNameToTaskRequest.taskName
      val taskRequest = taskNameToTaskRequest.createTaskNewInvestigationBody
      val taskLoadIO = loadTask(taskRequest, loginURL, explorerURL, username, password)(httpClient)
      TaskNameToTaskWorkId(taskName, taskLoadIO.unsafeRunSync().value)
    })
  }
}
