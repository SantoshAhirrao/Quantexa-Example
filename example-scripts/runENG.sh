#!/bin/bash

# Script for running Quantexa smoke test/project example on YARN cluster
# Example usage: ./runENG.sh resolver-config-fiu-smoke.json engConfig.conf

version=1.0.0 # version of ENG shadow JAR to use (use * to just pick up any version present - must only be one version present though)

if [ -z $jarDir ]
then
    jarDir=./Jars
fi

spark-submit \
	--class com.quantexa.engSpark.EngSpark \
	--master yarn \
	--num-executors 6 \
	--executor-cores 1 \
	--executor-memory 9g \
	--driver-memory 9g \
	--driver-cores 3 \
	--conf spark.sql.shuffle.partitions=2005 \
	--conf spark.driver.maxResultSize=4g \
	--conf spark.yarn.executor.memoryOverhead=2048 \
	--conf "spark.executor.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35" \
	$jarDir/quantexa-eng-spark-core-shadow_2.11-$version-all.jar $*