#!/bin/bash -x

set -e
exec >&2

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

cd ${WORKSPACE}/qa/env/mount/scripts

cat ./header.qson

echo "[91mExtract data models[0m"

mkdir -p ../etlengoutput/scoring
cp -r ../data/scoring/* ../etlengoutput/scoring

bash -x ./runETLSmoke.sh
