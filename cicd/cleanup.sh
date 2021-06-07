#!/bin/bash

function cleanup_eng() {
    if [ -d results ]
    then
	rm -rf ./results/*
    fi
}

cd ../qa/env
rm -rf ./mount/etlengoutput/customer
rm -rf ./mount/etlengoutput/hotlist
rm -rf ./mount/etlengoutput/transaction
rm -rf ./mount/etlengoutput/scoring
cleanup_eng

