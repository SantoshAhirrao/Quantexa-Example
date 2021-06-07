#!/bin/bash -x

set -e
exec >&2

CREATE_TASK_LIST=$1

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

cd ${WORKSPACE}/qa/env/mount/scripts

echo "[91mRunning Graph Scripts....[0m"

bash -x ./runTaskLoading.sh $CREATE_TASK_LIST