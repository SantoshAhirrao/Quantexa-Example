#!/bin/bash

# Script for running Quantexa smoke test/project example on YARN cluster
# Example usage: ./runScoring.sh -s com.quantexa.example.scoring.batch.utils.fiu.RunBatchScoringFramework -c scoringConfig.conf

if [ -z $jarDir ]
then
    jarDir=./Jars
fi

if [ -z $version ]
then
    version=*
fi

if [ -z $hdfsRoot ]
then
    hdfsRoot=/user/quantexa
fi

if [ -z $scoresToRun ]
then
    scoresToRun=all
fi

spark-submit \
	--class com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
	--master yarn \
	--num-executors 2 \
	--executor-cores 3 \
	--executor-memory 6g \
	--driver-cores 3 \
	--driver-memory 4g \
    --conf "spark.ui.port=0" \
	--conf "spark.executor.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35 -DengSparkRoot=$hdfsRoot/eng -DscoreOutputRoot=$hdfsRoot/scoring/ -DrunBatchScore=$scoresToRun" \
	--conf "spark.driver.extraJavaOptions=-DengSparkRoot=$hdfsRoot/eng -DscoreOutputRoot=$hdfsRoot/scoring/ -DrunBatchScore=$scoresToRun" \
	--conf "spark.driver.extraClassPath=/usr/lib/hadoop-lzo/lib/*:./" \
	--jars $(find $jarDir -name "example-scoring-batch-shadow-$version-dependency.jar" | tr '\n' ',' | sed 's/,$//'),$(find $jarDir -name "example-scoring-batch-shadow-$version-projects.jar" | tr '\n' ',' | sed 's/,$//')\
	$jarDir/example-scoring-batch-shadow-$version-projects.jar "$@"
