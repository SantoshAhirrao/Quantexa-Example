#!/bin/bash -x

set -e
exec >&2

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

cd $WORKSPACE

echo "[91mRunning Graph Scripts....[0m"

./gradlew example-graph-scripting:graph-script-batch:graphScriptTest -Dgateway.location="kub"