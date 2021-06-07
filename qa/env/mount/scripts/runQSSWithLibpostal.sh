#!/usr/bin/env bash

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

jarDir=${WORKSPACE}/qa/env/mount/jars
version=*
libpostalDir=/quantexa/libpostal_1.0 # jpostal.jar, joint.tar.gz and libpostal_datadir.tar.gz expected in this location

spark-submit \
	--class com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
	--master local[1] \
	--num-executors 4 \
    --executor-cores 1 \
    --executor-memory 2g \
	--conf spark.sql.shuffle.partitions=20 \
	--conf spark.driver.maxResultSize=1g \
	--conf "spark.driver.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35 -DlibpostalDataDir=$libpostalDir/libpostal/ -Dderby.system.home=/tmp" \
    --conf "spark.driver.extraLibraryPath=$libpostalDir/joint/" \
    --conf "spark.driver.extraClassPath=/usr/lib/hadoop-lzo/lib/*:./" \
	--jars  /jpostal/build/libs/jpostal.jar\
	$jarDir/etl-all-shadow-$version-all.jar "$@"



