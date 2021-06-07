package com.quantexa.example.taskloading.config

case class ApplicationConfig(
                              baseURL: String,
                              username: String,
                              password: String
                            ) {

  val loginURL = s"${baseURL}/api/authenticate"
  val explorerURL = s"${baseURL}/explorer"
  val securityURL = s"${baseURL}/app-security"

}

case class TaskLoadingConfig(
                              projectRoot: String,
                              runId: String,
                              applicationConfig: ApplicationConfig,
                              taskListName: String,
                              alertScoreThreshold: Int,
                              daysNotToReAlert: Int
                            ) {

  val tasksRoot = s"$projectRoot/tasks"
  val scoringRoot = s"$projectRoot/scoring"
  val graphScriptingRoot = s"$projectRoot/graphscripting"
  val customerRoot = s"$projectRoot/customer/$runId"

}