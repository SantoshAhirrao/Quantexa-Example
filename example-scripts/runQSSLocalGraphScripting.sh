#!/bin/bash

if [ -z $jarDir ]
then
    jarDir=./Jars
fi

VERSION="1.0.2-SNAPSHOT"

spark-submit \
	--class com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
	--master local[1] \
	--num-executors 2 \
	--executor-cores 3 \
	--executor-memory 6g \
	--driver-cores 3 \
	--driver-memory 4g \
	--conf "spark.ui.port=0" \
	--conf "spark.executor.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35" \
	--conf "spark.driver.extraClassPath=/usr/lib/hadoop-lzo/lib/*:./" \
	--packages com.crealytics:spark-excel_2.11:0.9.15 \
	--jars "$jarDir/example-graph-scripting-batch-shadow-$VERSION-dependency.jar,$jarDir/example-graph-scripting-batch-shadow-$VERSION-projects.jar" \
    $jarDir/example-graph-scripting-batch-shadow-$VERSION-dependency.jar "$@"