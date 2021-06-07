package com.quantexa.example.taskloading.batch

import java.sql.Date

import com.quantexa.example.taskloading.config.Constants._
import com.quantexa.example.taskloading.config.TaskLoadingConfig
import com.quantexa.example.taskloading.model.Model._
import com.quantexa.example.taskloading.model.ProjectModel.{ScoreInformation, ScorecardOutputInformation, TaskInformationFinal, TaskInformationWorking}
import com.quantexa.scriptrunner.util.metrics.ETLMetricsRepository
import com.quantexa.scriptrunner.{QuantexaSparkScript, TypedSparkScript}
import io.circe.generic.auto.exportDecoder
import org.apache.log4j.Logger
import org.apache.spark.sql.{Dataset, SparkSession}

/**
  * Select which customers should have tasks raised this run
  *
  * Inputs:
  * Scorecard output from this run
  * Consolidated view of previous tasks successfully loaded
  *
  * Condition for selecting customers to create tasks for is based on business logic
  *
  * The consolidated view of tasks raised is updated with tasks from
  * this run in a later job, once tasks have been loaded
  */
object SelectCustomersToRaiseTasksFor extends TypedSparkScript[TaskLoadingConfig] {

  val name = "Select Customers To Raise Tasks For"

  def fileDependencies: Map[String, String] = Map.empty

  def scriptDependencies = Set.empty[QuantexaSparkScript]

  def run(spark: SparkSession, logger: Logger, args: Seq[String], config: TaskLoadingConfig, etlMetricsRepository: ETLMetricsRepository) = {

    import spark.implicits._

    /*--------------- CONFIG AND PATHS ---------------*/

    val scoringRoot = config.scoringRoot
    val tasksRoot = config.tasksRoot

    val tasksThisRunRoot = tasksRoot
    val taskPreviousRunRoot: Option[String] = None // In reality, would be populated or set to None if initial run
    val scoringThisRunRoot = scoringRoot

    // Input paths
    val scorecardOutputThisRunPath = s"${scoringThisRunRoot}/ScoreCard/CustomerScorecard"
    val consolidatedTaskInfoPath: Option[String] = taskPreviousRunRoot.map(previousRoot => s"${previousRoot}/ConsolidatedTaskInfo.parquet")

    // Output paths
    val selectedTaskInfoThisRunPath = s"$tasksThisRunRoot/SelectedTaskInfoThisRun.parquet"

    // Other metadata/config
    val runDate = new java.sql.Date(System.currentTimeMillis())
    val scoringThreshold = config.alertScoreThreshold
    val daysToNotReAlert = config.daysNotToReAlert

    /*------------------------------------------------*/

    val scorecardOutputThisRun = spark.read.parquet(scorecardOutputThisRunPath).as[CustomWeightedStandardisedScoreOutput]

    val consolidatedTaskInformationPreviousRuns = consolidatedTaskInfoPath.map(
      spark.read.parquet(_).as[TaskInformationFinal].map(_.toTaskInformationWorking())
    ).getOrElse(Seq.empty[TaskInformationWorking].toDS)
    logger.info(s"consolidatedTaskInformationPreviousRuns Count : ${consolidatedTaskInformationPreviousRuns.count}")

    val taskInformationThisRun = scorecardOutputThisRun.map(createTaskInformation(_, runDate))
    logger.info(s"taskInformationThisRun Count : ${taskInformationThisRun.count}")

    val joinedTaskInformation = consolidatedTaskInformationPreviousRuns.union(taskInformationThisRun)

    val joinedTaskInformationGrouped = groupBySubject(joinedTaskInformation).cache
    logger.info(s"joinedTaskInformationGrouped Count : ${joinedTaskInformationGrouped.count}")

    // Remove customers not in scorecard this run
    val joinedTaskInformationGroupedUnderConsideration =
      joinedTaskInformationGrouped.filter { ds => hasTaskUnderConsideration(ds._2) }
    logger.info(s"joinedTaskInformationGroupedUnderConsideration Count : ${joinedTaskInformationGroupedUnderConsideration.count}")

    val raiseTasksFor = joinedTaskInformationGroupedUnderConsideration.map {
      case (customerId, taskInfo) => (customerId, raiseTaskForCustomer(taskInfo, scoringThreshold,
        daysToNotReAlert, runDate), taskInfo)
    }.filter {
      _._2
    }.cache
    logger.info(s"raiseTasksFor Count : ${raiseTasksFor.count}")

    val selectedTaskInfoThisRun = raiseTasksFor
      .map(ds => taskInfoUnderConsideration(ds._3)) // Extract task info from this run
      .map(taskInfo => taskInfo.copy(underConsideration = false))

    selectedTaskInfoThisRun.write.mode("overwrite").parquet(selectedTaskInfoThisRunPath)

  }

  /**
    * All business logic for determining whether to raise an alert for a customer
    */
  def raiseTaskForCustomer(taskInfo: Seq[TaskInformationWorking], scoreThreshold: Int,
                           daysToNotReAlert: Int, currentDate: Date): Boolean = {

    val taskUnderConsideration = taskInfoUnderConsideration(taskInfo)

    val scoreAboveAlertingThreshold = hasScoreGreaterThanThreshold(taskUnderConsideration, scoreThreshold)
    val noTaskRaisedInDaysToNotReAlert = !latestTaskRaiseDateWithinNDays(taskInfo, currentDate, daysToNotReAlert)
    val totalScoreGreaterThanLatestTask = totalScoreGreaterThanLatestPreviousTask(taskUnderConsideration, taskInfo)
    val newScoreTriggeredComparedToLatestTask = newScoreTriggeredComparedToLatestPreviousTask(taskUnderConsideration, taskInfo)

    scoreAboveAlertingThreshold &&
      (noTaskRaisedInDaysToNotReAlert || totalScoreGreaterThanLatestTask || newScoreTriggeredComparedToLatestTask)
  }

  /** Returns true if score greater than threshold */
  def hasScoreGreaterThanThreshold(taskInfo: TaskInformationWorking, scoreThreshold: Int): Boolean = {
    taskInfo.scorecardOutput.scorecardScore > scoreThreshold
  }

  /**
    * Returns true if total score is greater than score of latest task raised
    * If no latest task returns false
    * If latest task has a score less than or equal to task under consideration returns false
    */
  def totalScoreGreaterThanLatestPreviousTask(taskUnderConsideration: TaskInformationWorking, taskInfo: Seq[TaskInformationWorking]): Boolean = {
    val latestTask = latestTaskRaised(taskInfo)
    val isGreater = latestTask.map(lt => taskUnderConsideration.scorecardOutput.scorecardScore > lt.scorecardOutput.scorecardScore)
    isGreater.contains(true)
  }

  /**
    * Returns true if scores have fired in task under consideration not in latest task raised
    * If no latest task return false
    * If no new scores fired returns false
    */
  def newScoreTriggeredComparedToLatestPreviousTask(taskUnderConsideration: TaskInformationWorking, taskInfo: Seq[TaskInformationWorking]): Boolean = {
    val latestTask = latestTaskRaised(taskInfo)

    /*
    Identify 'scores' and not 'risk factors'
    We use candidate contribution as which score in a group actually contributes to
    the scorecard is non-deterministic between runs in the case that multiple scores have the
    same candidate contribution
    */
    def contributingScoreIds(ti: TaskInformationWorking): Set[String] = {
      ti.scorecardOutput.scoreInformation.filter(_._2.candidateContribution.getOrElse(0D) > 0).keys.toSet
    }

    val hasNewScores = latestTask.map(lt => {
      val scoresTriggeredLatestTask = contributingScoreIds(lt)
      val scoreTriggeredUnderConsideration = contributingScoreIds(taskUnderConsideration)
      scoreTriggeredUnderConsideration.diff(scoresTriggeredLatestTask).nonEmpty
    })
    hasNewScores.contains(true)
  }

  /** Returns true if the latest task raised is within N days of specified current date
    * If no previous task returns false
    * If latest task longer than N days ago returns false
    */
  def latestTaskRaiseDateWithinNDays(taskInfo: Seq[TaskInformationWorking], currentDate: Date, days: Int): Boolean = {
    val lastDate = latestTaskRaisedDate(taskInfo)
    val daysBetween = lastDate.map(lastDate => {
      daysDiff(currentDate, lastDate)
    })
    daysBetween.map(_ < days).contains(true)
  }

  /** Required for ordering SQL dates */
  implicit val sqlDateOrdering = new Ordering[Date] {
    def compare(x: Date, y: Date): Int = x compareTo y
  }

  /** Returns information about the latest task raised */
  def latestTaskRaised(taskInfo: Seq[TaskInformationWorking]): Option[TaskInformationWorking] = {
    taskInfo.filterNot(_.underConsideration).sortBy(_.date).reverse.headOption
  }

  /** Returns the date of the latest task raised */
  def latestTaskRaisedDate(taskInfo: Seq[TaskInformationWorking]): Option[Date] = {
    latestTaskRaised(taskInfo).map(_.date)
  }

  /** Calculates the number of days difference between two dates */
  def daysDiff(d1: Date, d2: Date): Double = {
    val millisecondsInDay = 1000D * 60 * 60 * 24
    val diffMilliseconds = d1.getTime() - d2.getTime()
    diffMilliseconds / millisecondsInDay
  }

  def hasTaskUnderConsideration(taskInfo: Seq[TaskInformationWorking]): Boolean = {
    taskInfo.filter(_.underConsideration).size match {
      case 1 => true
      case 0 => false
      case _ => throw new IllegalStateException(s"More than 1 Current Task Info Under Consideration: ${taskInfo}")
    }
  }

  def taskInfoUnderConsideration(taskInfo: Seq[TaskInformationWorking]): TaskInformationWorking = {
    taskInfo.filter(_.underConsideration).size match {
      case 1 => taskInfo.filter(_.underConsideration).head
      case 0 => throw new IllegalStateException(s"No Current Task Info Under Consideration: ${taskInfo}")
      case _ => throw new IllegalStateException(s"More than 1 Current Task Info Under Consideration: ${taskInfo}")
    }
  }

  def groupBySubject(ds: Dataset[TaskInformationWorking]): Dataset[(String, Seq[TaskInformationWorking])] = {
    import ds.sparkSession.implicits._
    ds.groupByKey(ds => ds.customerId).mapGroups {
      case (id, iter) => {
        (id, iter.toSeq)
      }
    }
  }

  def createTaskInformation(wso: CustomWeightedStandardisedScoreOutput, date: Date): TaskInformationWorking = {
    TaskInformationWorking(
      customerId = wso.subjectId.get, // This must be defined
      date = date,
      scorecardOutput = createScorecardOutputInformation(wso),
      rolesAbleToSeeTask = rolesAbleToSeeTask(wso),
      taskListId = None, // Representing a potential task at this point; no task loaded
      taskListName = None, // Representing a potential task at this point; no task loaded
      underConsideration = true)
  }

  def createScorecardOutputInformation(wso: CustomWeightedStandardisedScoreOutput): ScorecardOutputInformation = {
    val scoreInformation = wso.weightedScoreOutputMap.mapValues {
      ws => {
        ScoreInformation(
          candidateContribution = ws.candidateContribution,
          contribution = ws.contribution,
          contributing = ws.contributesToScorecardScore)
      }
    }
    ScorecardOutputInformation(scorecardScore = wso.scorecardScore,
      scoreInformation = scoreInformation)
  }

  /**
    * Determines the roles able to see a task based upon a score
    * Score used as we need to check for sensitivity of customers on network as well as focal customer
    */
  def rolesAbleToSeeTask(wso: CustomWeightedStandardisedScoreOutput): Seq[String] = {
    Seq(NON_SENSITIVE_ROLE, SENSITIVE_ROLE)
  }
}
