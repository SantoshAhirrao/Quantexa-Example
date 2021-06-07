#!/bin/bash -x
 
set -e
exec >&2

echo copying over jars...

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

cd $WORKSPACE 

mkdir -p ./qa/env/mount/jars

cp ./data-source-all/etl-all/build/libs/etl-all-shadow-*-all.jar ./qa/env/mount/jars/
cp ./example-scoring-batch/build/libs/example-scoring-batch-shadow-*-projects.jar ./qa/env/mount/jars/
cp ./example-scoring-batch/build/libs/example-scoring-batch-shadow-*-dependency.jar ./qa/env/mount/jars/
cp ./example-graph-scripting/graph-script-batch/build/libs/graph-script-batch-shadow-*-projects.jar ./qa/env/mount/jars/
cp ./example-graph-scripting/graph-script-batch/build/libs/graph-script-batch-shadow-*-dependency.jar ./qa/env/mount/jars/
cp ./example-data-generator/build/libs/example-data-generator-shadow-*-dependency.jar ./qa/env/mount/jars/
cp ./example-data-generator/build/libs/example-data-generator-shadow-*-projects.jar ./qa/env/mount/jars/
cp ./eng-spark-core-shadow/build/libs/quantexa-eng-spark-core-shadow_2.11-*-all.jar ./qa/env/mount/jars/
cp ./example-task-loading/task-loading-batch/build/libs/task-loading-batch-shadow-*-dependency.jar ./qa/env/mount/jars/
cp ./example-task-loading/task-loading-batch/build/libs/task-loading-batch-shadow-*-projects.jar ./qa/env/mount/jars/
