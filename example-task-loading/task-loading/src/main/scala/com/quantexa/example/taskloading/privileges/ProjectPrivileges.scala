package com.quantexa.example.taskloading.privileges

import cats.data.NonEmptyList
import com.quantexa.acl.AclModel
import com.quantexa.acl.AclModel.{PrivilegeSetId, PrivilegeSetName, SubjectPrivilegeSetMapping}
import com.quantexa.example.taskloading.config.Constants.{SYSTEM_USER, USER_BASE_ROLE}
import com.quantexa.example.taskloading.model.Model.TaskPrivilegeSetCreation
import com.quantexa.example.taskloading.model.ProjectModel
import com.quantexa.explorer.tasks.api.Model.TaskListDefinitionId
import com.quantexa.explorer.tasks.api.security.TaskListPrivileges._
import com.quantexa.explorer.tasks.api.security.TaskPrivileges._
import com.quantexa.explorer.tasks.api.security.{ExtraDataPrivileges, TaskOutcomePrivileges}
import com.quantexa.graph.script.utils.TaskUtils.allPrivilegesForDefinition
import com.quantexa.security.api.SecurityModel.{RoleId, UserId}

/**
  * All privilege related methods/constants are defined in this object
  */
object ProjectPrivileges {

  // All privileges
  def allPrivilegesFunc(taskListDefinitionId: TaskListDefinitionId): Set[AclModel.Privilege] = {
    allPrivilegesForDefinition(taskListDefinitionId, ProjectModel.extraDataSchema, ProjectModel.outcomeDataSchema)
  }

  // Privileges required to open a task list - does not assign the privileges required to actually see a task
  def userTaskListPrivilegesFunc(taskListDefinitionId: TaskListDefinitionId): Set[AclModel.Privilege] = {
    val extraDataPrivileges = ExtraDataPrivileges.all(taskListDefinitionId, ProjectModel.extraDataSchema).map(_.asAclPrivilege)
    val outcomeDataPrivileges = TaskOutcomePrivileges.all(taskListDefinitionId, ProjectModel.outcomeDataSchema).map(_.asAclPrivilege)
    val taskPrivileges = Set(GetTask, GetTaskVersions, GetTaskPrivileges, GetTaskUIPrivileges).map(_.asAclPrivilege)
    val taskListPrivileges = Set(GetTaskList, GetTaskLists, GetTaskListMetadata, GetTaskListPrivileges).map(_.asAclPrivilege)
    taskListPrivileges ++ taskPrivileges ++ extraDataPrivileges ++ outcomeDataPrivileges
  }

  /*
   * Almost all privileges
   * More than should be granted to a standard user in general
   * Here we are only preventing a user from changing the privileges on a task/investigation
   * Most projects would probably want to prevent users from deleting tasks etc.
   */
  def userTaskPrivilegesFunc(taskListDefinitionId: TaskListDefinitionId): Set[AclModel.Privilege] = {
    val all = allPrivilegesForDefinition(taskListDefinitionId, ProjectModel.extraDataSchema, ProjectModel.outcomeDataSchema)
    val updatePrivilegesTask = UpdateTaskPrivileges.asAclPrivilege
    val updatePrivilegesInvestigation = UpdateTaskPrivileges.asAclPrivilege
    all -- Set(updatePrivilegesTask, updatePrivilegesInvestigation)
  }

  val ALL_PRIVILEGES_NAME = "All-Privileges"
  val USER_TASK_LIST_PRIVILEGES_NAME = "User-Task-List-Privileges"
  val USER_TASK_PRIVILEGES_NAME = "User-Task-Privileges"

  val allPrivileges = TaskPrivilegeSetCreation(taskPrivilegeSetName = ALL_PRIVILEGES_NAME, privilegeSetFunction = allPrivilegesFunc)
  val userTaskListPrivileges = TaskPrivilegeSetCreation(taskPrivilegeSetName = USER_TASK_LIST_PRIVILEGES_NAME, privilegeSetFunction = userTaskListPrivilegesFunc)
  val userTaskPrivileges = TaskPrivilegeSetCreation(taskPrivilegeSetName = USER_TASK_PRIVILEGES_NAME, privilegeSetFunction = userTaskPrivilegesFunc)

  val projectPrivileges = NonEmptyList.of(allPrivileges, userTaskListPrivileges, userTaskPrivileges)

  // Function to create the privilege set mapping for a task
  def taskPrivilegeSetMapping(taskPrivilegeSets: collection.Map[PrivilegeSetName, PrivilegeSetId], allowedRoles: Seq[String]): SubjectPrivilegeSetMapping = {
    val userTaskPrivilegeSet = Set(taskPrivilegeSets(PrivilegeSetName(USER_TASK_PRIVILEGES_NAME)))
    val roleMap = allowedRoles.map(new RoleId(_)).map((_, userTaskPrivilegeSet)).toMap
    val userMap = Map(new UserId(SYSTEM_USER) -> Set(taskPrivilegeSets(PrivilegeSetName(ALL_PRIVILEGES_NAME))))
    new SubjectPrivilegeSetMapping(roleMap, userMap)
  }

  // Function to create the privilege set mapping for a task list
  def taskListPrivilegeSetMapping(taskPrivilegeSets: collection.Map[PrivilegeSetName, PrivilegeSetId]): SubjectPrivilegeSetMapping = {
    new SubjectPrivilegeSetMapping(
      Map(new RoleId(USER_BASE_ROLE) -> Set(taskPrivilegeSets(PrivilegeSetName(USER_TASK_LIST_PRIVILEGES_NAME)))),
      Map(new UserId(SYSTEM_USER) -> Set(taskPrivilegeSets(PrivilegeSetName(ALL_PRIVILEGES_NAME))))
    )
  }

}
