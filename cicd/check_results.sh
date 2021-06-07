#!/bin/bash

function checkResults() {
    cat ./results.log
    if [ `grep -c PASS ./results.log` -ge $num_tests ]
    then
        exit 0
    else
        exit 1
    fi
}

if [ -z $WORKSPACE ]
then
    WORKSPACE=`dirname $0`/..
fi

smoke_test=$WORKSPACE/qa/env/mount/tests/$1
num_tests=`ls -l $smoke_test| grep '.sh'|wc -l`

cd $smoke_test
while [ ! -e ./results.log ]
do
    sleep 10
done

checkResults
