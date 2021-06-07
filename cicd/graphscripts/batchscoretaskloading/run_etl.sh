#!/bin/bash -x

set -e
exec >&2

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

cd ${WORKSPACE}/qa/env/mount/scripts
CONFIG_DIR=${WORKSPACE}/qa/env/mount/config

cat ./header.qson

echo "[91mExtract data models[0m"

mkdir -p ../etlengoutput/scoring
cp -r ../data/scoring/* ../etlengoutput/scoring

sed -i "s%\/home\/jenkins\/workspace\/Accelerators\/project-example-folder\/project-example-daily-build%${WORKSPACE}%g" $CONFIG_DIR/resolver-config-fiu-smoke.json
sed -i "s%\/home\/jenkins\/workspace\/Accelerators\/project-example-folder\/project-example-daily-build%${WORKSPACE}%g" $CONFIG_DIR/resolver-config-fiu-smoke-eng.json
sed -i "s%\/home\/jenkins\/workspace\/Accelerators\/project-example-folder\/project-example-daily-build%${WORKSPACE}%g" $CONFIG_DIR/engSmoke.conf

bash -x ./runETLSmoke.sh
