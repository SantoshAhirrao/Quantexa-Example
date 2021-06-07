#!/bin/bash

set -e

if [ -z $WORKSPACE ]
then
    WORKSPACE=`dirname $0`/..
fi

tests=${WORKSPACE}/qa/env/mount/tests/$1

cd $tests
if [ -e ./results.log ]
then
    rm ./results.log
fi

for testsuite in `ls .|grep sh`
do
    echo executing $testsuite
    ./$testsuite >> ./results.log
done

# await test results
sleep 5
