#!/bin/bash

# Script for running Quantexa smoke test/project example etl, eng and scoring
# Example usage: ./runAllEtl.sh

set -a

username=<USEERNAME HERE>
hdfsRoot=/user/$username # Home location of smoke test
version=1.0.0-SNAPSHOT # Version of smoke test to use
scoresToRun=all # Runs all batch scores containing this variable. Can also denote a section of scores (customerscores, transactionscores etc)
jarDir=./Jars # Scripts will look for ETL project and ETL dependency JARs in this location
libpostalDir=/quantexa/libpostal_1.0 # jpostal.jar, joint.tar.gz and libpostal_datadir.tar.gz expected in this location
configVersion=dev # Config files are expected to be in same directory as this script

echo -e "\e[32mWorking Out Elastic IP Address\e[0m"
iteration=0
while [[ $output != *"worker"* ]]; do iteration=$[$iteration+1]; output=$(curl -s -m 1 http://10.17.0.$iteration:9200/_cat/nodes); done;
sed -i "s/\[\".*/\[\"${output/ *}:9200\"\]/g" ./external.conf
echo -e "\e[32mElastic worker at ${output/ *}\e[0m"

echo -e "\e[32mRunning all ETL\e[0m"
./runAllEtl.sh

echo -e "\e[32mRunning ENG\e[0m"
./runENG.sh resolver-config-fiu-smoke-eng.json engConfig.conf

echo -e "\e[32mRunning all Scoring\e[0m"
./runScoring.sh -s com.quantexa.example.scoring.batch.utils.fiu.RunScoresInSparkSubmit -e $configVersion -r scoring.fiu

echo -e "\e[32mProject Example ETL ENG and Scoring Successful\e[0m"

