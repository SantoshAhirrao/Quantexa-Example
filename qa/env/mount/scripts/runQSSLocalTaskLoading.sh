#!/bin/bash

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

jarDir=${WORKSPACE}/qa/env/mount/jars
version=*

spark-submit \
	--class com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
	--master local[*] \
	--conf "spark.executor.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35 -Djavax.net.ssl.trustStore=$TRUST_STORE" \
    --conf spark.driver.extraJavaOptions=-Djavax.net.ssl.trustStore=$TRUST_STORE \
	--conf "spark.driver.extraClassPath=/usr/lib/hadoop-lzo/lib/*:./" \
	--packages com.crealytics:spark-excel_2.11:0.9.15 \
	--jars $(find $jarDir -name "task-loading-batch-shadow-$version-dependency.jar" | tr '\n' ',' | sed 's/,$//'),$(find $jarDir -name "task-loading-batch-shadow-$version-projects.jar" | tr '\n' ',' | sed 's/,$//')\
    $jarDir/task-loading-batch-shadow-$version-projects.jar "$@"