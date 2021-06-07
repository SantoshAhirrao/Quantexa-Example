#!/bin/bash

# Script for running Quantexa smoke test/project example on YARN cluster
# Example usage: ./runQSSWithLibpostal.sh -s com.quantexa.example.etl.projects.fiu.customer.CleanseCaseClass -e dev -r etl

if [ -z $jarDir ]
then
    jarDir=./Jars
fi

if [ -z $version ]
then
    version=*
fi

if [ -z $libpostalDir ]
then
    libpostalDir=/quantexa/libpostal_1.0
fi

spark-submit \
	--class com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
	--master yarn \
	--num-executors 1 \
	--executor-memory 4g \
	--driver-cores 3 \
	--driver-memory 4g \
	--conf "spark.ui.port=0" \
	--conf "spark.executor.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35 -DlibpostalDataDir=./libpostal_datadir.tar.gz" \
	--conf "spark.executor.extraLibraryPath=./joint.tar.gz" \
	--conf "spark.yarn.dist.archives=$libpostalDir/joint.tar.gz,$libpostalDir/libpostal_datadir.tar.gz" \
	--conf "spark.driver.extraClassPath=/usr/lib/hadoop-lzo/lib/*:./" \
	--packages com.crealytics:spark-excel_2.11:0.9.15 \
	--jars $(find $jarDir -name "etl-all-shadow-$version-all.jar" | tr '\n' ',' | sed 's/,$//'),$(find $libpostalDir -name "jpostal.jar" | tr '\n' ',' | sed 's/,$//')\
	$jarDir/etl-all-shadow-$version-all.jar "$@"