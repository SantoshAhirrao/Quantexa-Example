#!/bin/bash -x

set -e
exec >&2

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

cd ${WORKSPACE}/qa/env/mount/scripts

cat ./header.qson

echo "[91mRunning scoring pipeline[0m"

bash -x ./runScoring.sh
