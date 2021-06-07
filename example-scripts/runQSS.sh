#!/bin/bash

# Script for running Quantexa smoke test/project example on YARN cluster, without Libpostal
# Example usage: ./runQSS.sh -s com.quantexa.example.etl.projects.fiu.customer.ImportRawToParquet -e dev -r etl

if [ -z $jarDir ]
then
    jarDir=./Jars
fi

if [ -z $version ]
then
    version=*
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
	--conf "spark.executor.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35" \
	--conf "spark.driver.extraClassPath=/usr/lib/hadoop-lzo/lib/*:./" \
	--packages com.crealytics:spark-excel_2.11:0.9.15 \
	--jars $(find $jarDir -name "etl-all-shadow-$version-all.jar" | tr '\n' ',' | sed 's/,$//')\
    $jarDir/etl-all-shadow-$version-all.jar "$@"