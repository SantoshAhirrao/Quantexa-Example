package com.quantexa.example.taskloading.batch

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import java.sql.Date

import com.quantexa.example.taskloading.model.Model._
import com.quantexa.example.taskloading.batch.SelectCustomersToRaiseTasksFor._
import com.quantexa.example.taskloading.model.ProjectModel.{ScoreInformation, ScorecardOutputInformation, TaskInformationWorking}

class SelectCustomersToRaisedTasksForTests extends FlatSpec with Matchers {

  "daysDiff" should "give the correct date difference test between two dates with the same time" in {

    val sqlDate1 = new Date(1546344000000L) // Tuesday, January 1, 2019 1:00:00 PM GMT+01:00
    val sqlDate2 = new Date(1546516800000L) // Thursday, January 3, 2019 1:00:00 PM GMT+01:00

    assert(daysDiff(sqlDate2, sqlDate1) == 2)
  }

  "daysDiff" should "give the correct date difference between two dates with a different time" in {

    val sqlDate1 = new Date(1546344000000L) // Tuesday, January 1, 2019 1:00:00 PM GMT+01:00
    val sqlDate2 = new Date(1546552800000L) // Thursday, January 3, 2019 11:00:00 PM GMT+01:00

    assert(daysDiff(sqlDate2, sqlDate1) == 2.4166666666666665)
  }

  "daysDiff" should "give the correct date difference when dates are the same" in {

    val sqlDate1 = new Date(1546344000000L) // Tuesday, January 1, 2019 1:00:00 PM GMT+01:00
    val sqlDate2 = new Date(1546344000000L) // Tuesday, January 1, 2019 1:00:00 PM GMT+01:00

    assert(daysDiff(sqlDate2, sqlDate1) == 0)
  }

  "latestTaskRaised" should "correctly identify the latest task raised" in {

    val underConsideration = TaskInformationWorking(
      customerId = "12345",
      date = new Date(300000000000L),
      scorecardOutput = ScorecardOutputInformation(0, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = true)

    val latest = TaskInformationWorking(
      customerId = "12345",
      date = new Date(200000000000L),
      scorecardOutput = ScorecardOutputInformation(0, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = false)

    val notLatest = TaskInformationWorking(
      customerId = "12345",
      date = new Date(100000000000L),
      scorecardOutput = ScorecardOutputInformation(0, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = false)

    val taskInfo = Seq(underConsideration, latest, notLatest)

    assert(latestTaskRaised(taskInfo).map(_.date.getTime) == Some(200000000000L))
  }

  "latestTaskRaised" should "return None when there are no previous tasks" in {

    val underConsideration = TaskInformationWorking(
      customerId = "12345",
      date = new Date(300000000000L),
      scorecardOutput = ScorecardOutputInformation(0, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = true)

    val taskInfo = Seq(underConsideration)

    assert(latestTaskRaised(taskInfo).map(_.date.getTime) == None)
  }

  "latestTaskRaiseDateWithinNDays" should "return true when there is a task within N days" in {

    val underConsideration = TaskInformationWorking(
      customerId = "12345",
      date = new Date(300000000000L),
      scorecardOutput = ScorecardOutputInformation(0, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = true)

    val latest = TaskInformationWorking(
      customerId = "12345",
      date = new Date(299999900000L),
      scorecardOutput = ScorecardOutputInformation(0, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = false)

    val notLatest = TaskInformationWorking(
      customerId = "12345",
      date = new Date(100000000000L),
      scorecardOutput = ScorecardOutputInformation(0, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = false)

    val taskInfo = Seq(underConsideration, latest, notLatest)
    assert(latestTaskRaiseDateWithinNDays(taskInfo, new Date(300000000000L), 1) == true)

  }

  "latestTaskRaiseDateWithinNDays" should "return false when there is not a task within N days" in {

    val underConsideration = TaskInformationWorking(
      customerId = "12345",
      date = new Date(300000000000L),
      scorecardOutput = ScorecardOutputInformation(0, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = true)

    val latest = TaskInformationWorking(
      customerId = "12345",
      date = new Date(100000000000L),
      scorecardOutput = ScorecardOutputInformation(0, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = false)

    val taskInfo = Seq(underConsideration, latest)
    assert(latestTaskRaiseDateWithinNDays(taskInfo, new Date(300000000000L), 1) == false)

  }

  "latestTaskRaiseDateWithinNDays" should "return false when there is not a previous task" in {

    val underConsideration = TaskInformationWorking(
      customerId = "12345",
      date = new Date(300000000000L),
      scorecardOutput = ScorecardOutputInformation(1000, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = true)

    val taskInfo = Seq(underConsideration)
    assert(latestTaskRaiseDateWithinNDays(taskInfo, new Date(300000000000L), 1) == false)

  }


  "totalScoreGreaterThanLatestPreviousTask" should "return true when the score is higher than the previous task" in {

    val underConsideration = TaskInformationWorking(
      customerId = "12345",
      date = new Date(300000000000L),
      scorecardOutput = ScorecardOutputInformation(1000, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = true)

    val latest = TaskInformationWorking(
      customerId = "12345",
      date = new Date(100000000000L),
      scorecardOutput = ScorecardOutputInformation(999, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = false)

    val taskInfo = Seq(underConsideration, latest)
    assert(totalScoreGreaterThanLatestPreviousTask(underConsideration, taskInfo) == true)

  }

  "totalScoreGreaterThanLatestPreviousTask" should "return false when the score is lower than the previous task" in {

    val underConsideration = TaskInformationWorking(
      customerId = "12345",
      date = new Date(300000000000L),
      scorecardOutput = ScorecardOutputInformation(999, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = true)

    val latest = TaskInformationWorking(
      customerId = "12345",
      date = new Date(100000000000L),
      scorecardOutput = ScorecardOutputInformation(1000, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = false)

    val taskInfo = Seq(underConsideration, latest)
    assert(totalScoreGreaterThanLatestPreviousTask(underConsideration, taskInfo) == false)

  }

  "totalScoreGreaterThanLatestPreviousTask" should "return false when there are no previous tasks" in {

    val underConsideration = TaskInformationWorking(
      customerId = "12345",
      date = new Date(300000000000L),
      scorecardOutput = ScorecardOutputInformation(999, Map()),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = true)

    val taskInfo = Seq(underConsideration)
    assert(totalScoreGreaterThanLatestPreviousTask(underConsideration, taskInfo) == false)

  }

  "newScoreTriggeredComparedToLatestPreviousTask" should "return true when new scores have fired" in {

    val underConsideration = TaskInformationWorking(
      customerId = "12345",
      date = new Date(300000000000L),
      scorecardOutput = ScorecardOutputInformation(999, Map("Score1" -> ScoreInformation(Some(1.0), 2, true), "Score2" -> ScoreInformation(Some(1.0), 2, true))),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = true)

    val latest = TaskInformationWorking(
      customerId = "12345",
      date = new Date(100000000000L),
      scorecardOutput = ScorecardOutputInformation(1000, Map("Score1" -> ScoreInformation(Some(1.0), 2, true))),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = false)

    val taskInfo = Seq(underConsideration, latest)
    assert(newScoreTriggeredComparedToLatestPreviousTask(underConsideration, taskInfo) == true)

  }

  "newScoreTriggeredComparedToLatestPreviousTask" should "return false when no new scores have fired" in {

    val underConsideration = TaskInformationWorking(
      customerId = "12345",
      date = new Date(300000000000L),
      scorecardOutput = ScorecardOutputInformation(999, Map("Score1" -> ScoreInformation(Some(1.0), 2, true))),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = true)

    val latest = TaskInformationWorking(
      customerId = "12345",
      date = new Date(100000000000L),
      scorecardOutput = ScorecardOutputInformation(1000, Map("Score1" -> ScoreInformation(Some(1.0), 2, true), "Score2" -> ScoreInformation(Some(1.0), 2, true))),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = false)

    val taskInfo = Seq(underConsideration, latest)
    assert(newScoreTriggeredComparedToLatestPreviousTask(underConsideration, taskInfo) == false)

  }

  "newScoreTriggeredComparedToLatestPreviousTask" should "return false when there are no previous tasks" in {

    val underConsideration = TaskInformationWorking(
      customerId = "12345",
      date = new Date(300000000000L),
      scorecardOutput = ScorecardOutputInformation(999, Map("Score1" -> null)),
      rolesAbleToSeeTask = Seq(""),
      taskListId = None,
      taskListName = None,
      underConsideration = true)

    val taskInfo = Seq(underConsideration)
    assert(newScoreTriggeredComparedToLatestPreviousTask(underConsideration, taskInfo) == false)

  }
}
