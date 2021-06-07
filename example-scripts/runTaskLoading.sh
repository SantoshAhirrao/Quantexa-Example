#!/bin/bash

echo -e "\e[32mSelect Customers To Raise Tasks For\e[0m"
./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.SelectCustomersToRaiseTasksFor -r config -c taskLoading.conf

echo -e "\e[32mGenerate Tasks To Load\e[0m"
./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.GenerateTasksToLoad -r config -c taskLoading.conf

echo -e "\e[32mCreate Task Type Task Definition And Privileges\e[0m"
./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.CreateTaskTypeTaskDefinitionAndPrivileges -r config -c taskLoading.conf

echo -e "\e[32mCreate Task List\e[0m"
./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.CreateTaskList -r config -c taskLoading.conf

echo -e "\e[32mLoad Tasks\e[0m"
./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.LoadTasks -r config -c taskLoading.conf

echo -e "\e[32mConsolidate Task Info\e[0m"
./runQSSLocalTaskLoading.sh -s com.quantexa.example.taskloading.batch.ConsolidateTaskInfo -r config -c taskLoading.conf
