#!/bin/bash

CREATE_TASK_LIST=$1

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

CONFIG_FOLDER=${WORKSPACE}/qa/env/mount/config

jarDir=${WORKSPACE}/qa/env/mount/jars

echo -e "\e[32mSelect Customers To Raise Tasks For\e[0m"
./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.SelectCustomersToRaiseTasksFor -r tasks -c ${CONFIG_FOLDER}/taskLoading.conf || exit 1

echo -e "\e[32mGenerate Tasks To Load\e[0m"
./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.GenerateTasksToLoad -r tasks -c ${CONFIG_FOLDER}/taskLoading.conf || exit 1

if [ $CREATE_TASK_LIST = "true" ]
then
    echo -e "\e[32mCreate Task Type Task Definition And Privileges\e[0m"
    ./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.CreateTaskTypeTaskDefinitionAndPrivileges -r tasks -c ${CONFIG_FOLDER}/taskLoading.conf || exit 1

    echo -e "\e[32mCreate Task List\e[0m"
    ./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.CreateTaskList -r tasks -c ${CONFIG_FOLDER}/taskLoading.conf || exit 1
fi


echo -e "\e[32mLoad Tasks\e[0m"
./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.LoadTasks -r tasks -c ${CONFIG_FOLDER}/taskLoading.conf || exit 1

echo -e "\e[32mConsolidate Task Info\e[0m"
./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.ConsolidateTaskInfo -r tasks -c ${CONFIG_FOLDER}/taskLoading.conf || exit 1