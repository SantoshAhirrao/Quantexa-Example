#!/usr/bin/env bash

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

jarDir=${WORKSPACE}/qa/env/mount/jars
version=* # version of ENG shadow JAR to use (use * to just pick up any version present - must only be one version present though)

spark-submit \
	--class com.quantexa.engSpark.EngSpark \
	--master local[*] \
	--num-executors 6 \
	--executor-cores 1 \
	--executor-memory 9g \
	--driver-memory 9g \
	--driver-cores 3 \
	--conf spark.sql.shuffle.partitions=20 \
	--conf spark.driver.maxResultSize=2g \
	--conf spark.yarn.executor.memoryOverhead=2048 \
	--conf "spark.executor.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35" \
	$jarDir/quantexa-eng-spark-core-shadow_2.11-$version-all.jar $*
