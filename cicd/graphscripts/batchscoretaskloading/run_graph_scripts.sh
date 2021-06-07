#!/bin/bash -x

set -e
exec >&2

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

cd $WORKSPACE
akkaVersion=2.5.19
jarDir=${WORKSPACE}/qa/env/mount/jars

echo "[91mRunning Graph Scripts....[0m"

spark-submit -v \
--master local[*] \
--class com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
--packages com.typesafe.akka:akka-remote_2.11:$akkaVersion,com.typesafe.akka:akka-actor_2.11:$akkaVersion,com.typesafe.akka:akka-stream_2.11:$akkaVersion,com.typesafe.akka:akka-persistence_2.11:$akkaVersion,com.typesafe.akka:akka-protobuf_2.11:$akkaVersion \
--conf spark.executor.extraJavaOptions=-Djavax.net.ssl.trustStore=$TRUST_STORE \
--conf spark.driver.extraJavaOptions=-Djavax.net.ssl.trustStore=$TRUST_STORE \
--jars $jarDir/example-graph-scripting-batch-shadow-*-dependency.jar \
$jarDir/example-graph-scripting-batch-shadow-*-projects.jar -s com.quantexa.example.graph.scripting.batch.rest.BulkOneHop -e kub -r bulkloader