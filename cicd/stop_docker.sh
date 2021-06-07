#!/bin/bash -x

set -e
exec >&2

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

cd ${WORKSPACE}/qa/env
docker-compose down
