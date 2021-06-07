#!/bin/bash

# Script for running Quantexa smoke test/project example on YARN cluster
# Example usage: ./runQSSScoring.sh -s com.quantexa.example.scoring.batch.utils.fiu.RunBatchScoringFramework -c scoringConfig.conf

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

jarDir=${WORKSPACE}/qa/env/mount/jars
CONFIG_FOLDER=${WORKSPACE}/qa/env/mount/config/
version=* # version of ENG shadow JAR to use (use * to just pick up any version present - must only be one version present though)

hdfsRoot=${WORKSPACE}/qa/env/mount/etlengoutput/

if [ -z $scoresToRun ]
then
    scoresToRun=all
fi

spark-submit \
	--class com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
	--master local[*] \
	--conf spark.sql.shuffle.partitions=20 \
	--conf spark.driver.maxResultSize=1g \
    --conf "spark.ui.port=0" \
	--conf "spark.executor.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35 -DengSparkRoot=$hdfsRoot/customer/eng -DscoreOutputRoot=$hdfsRoot/scoring/ -DrunBatchScore=$scoresToRun" \
	--conf "spark.driver.extraJavaOptions=-DengSparkRoot=$hdfsRoot/customer/eng -DscoreOutputRoot=$hdfsRoot/scoring/ -DrunBatchScore=$scoresToRun" \
	--conf "spark.driver.extraClassPath=/usr/lib/hadoop-lzo/lib/*:./:$CONFIG_FOLDER" \
	--driver-memory 1566m \
	--jars $(find $jarDir -name "example-scoring-batch-shadow-$version-dependency.jar" | tr '\n' ',' | sed 's/,$//'),$(find $jarDir -name "example-scoring-batch-shadow-$version-projects.jar" | tr '\n' ',' | sed 's/,$//')\
	$jarDir/example-scoring-batch-shadow-$version-projects.jar "$@"
