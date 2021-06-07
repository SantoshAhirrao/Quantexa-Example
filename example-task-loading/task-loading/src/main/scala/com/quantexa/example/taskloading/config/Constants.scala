package com.quantexa.example.taskloading.config

object Constants {
  val SYSTEM_USER = "SUPER-ADMIN" // The all powerful system/admin user. Could also be a role.
  val USER_BASE_ROLE = "ROLE_USER" // Base role - used to allow all users to access task lists
  val NON_SENSITIVE_ROLE = "ROLE_USER" // Role that can't see sensitive tasks
  val SENSITIVE_ROLE = "ROLE_SUPER_ADMIN" // Role that can see all task

  val TASK_LOADING_SCORE_SET = "EmptyScoringModel" // Empty scoring model if you don't want scoring to run during task loading
}
