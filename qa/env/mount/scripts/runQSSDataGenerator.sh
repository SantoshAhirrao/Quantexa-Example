#!/usr/bin/env bash

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

jarDir=${WORKSPACE}/qa/env/mount/jars
version=*

spark-submit \
	--class com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
	--master local[*] \
	--conf spark.sql.shuffle.partitions=20 \
	--conf spark.driver.maxResultSize=1g \
	--conf "spark.driver.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35 -DlibpostalDataDir=./libpostal_datadir.tar.gz -DstubParser=true -Dderby.system.home=/tmp" \
	--conf "spark.driver.extraLibraryPath=./joint.tar.gz" \
	--conf "spark.driver.extraClassPath=/usr/lib/hadoop-lzo/lib/*:./:$MOUNT_FOLDER/config/" \
	--driver-memory 1566m \
	--jars $jarDir/example-data-generator-shadow-$version-dependency.jar \
	$jarDir/example-data-generator-shadow-$version-projects.jar "$@"
