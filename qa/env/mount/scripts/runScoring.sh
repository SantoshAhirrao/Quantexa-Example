#!/bin/bash

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

CONFIG_FOLDER=${WORKSPACE}/qa/env/mount/config

function scoring-batch() {
    echo "Running batch scoring"
    bash ./runQSS.sh -s com.quantexa.example.etl.projects.fiu.countrycorruption.ImportRawToParquet \
	-c ${CONFIG_FOLDER}/external.conf -r etl || exit 1
    bash ./runQSSScoring.sh -s com.quantexa.example.scoring.batch.utils.fiu.RunScoresInSparkSubmit \
        -c ${CONFIG_FOLDER}/external.conf -r scoring.fiu || exit 1
}

function load-rollup-to-elastic() {
    echo "Loading Customer Date Rollup Output to elastic"
    bash ./runQSSScoring.sh -s com.quantexa.example.scoring.batch.scores.fiu.integration.LoadRollupTxnCustomerDateScoresToCustomerLevelElastic \
        -c ${CONFIG_FOLDER}/external.conf -r elastic.txncustomerdatescoretocustomerrollup
    echo "Loading Transaction Score Output Rollup to elastic"
    bash ./runQSSScoring.sh -s com.quantexa.example.scoring.batch.scores.fiu.integration.LoadRollupTxnScoreToCustomerLevelToElastic \
        -c ${CONFIG_FOLDER}/external.conf -r elastic.txnscoretocustomerrollup
}

scoring-batch
load-rollup-to-elastic
