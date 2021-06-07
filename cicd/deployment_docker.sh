#!/bin/bash -x

set -e
exec >&2

echo deploying to Kubernetes ...

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi
cd $WORKSPACE

gcloud auth configure-docker

./gradlew dockerTagAndPush --max-workers=2 --no-daemon -Dorg.gradle.jvmargs="-Xms4g -Xmx5g"