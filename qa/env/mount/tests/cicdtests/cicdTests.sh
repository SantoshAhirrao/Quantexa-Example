#!/bin/bash -x

exec >2&

echo running cicd test...

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi
cd $WORKSPACE

./gradlew example-cicd-testing:cicdTest --max-workers=2 --no-daemon --rerun-tasks -Dwoodhouse=true