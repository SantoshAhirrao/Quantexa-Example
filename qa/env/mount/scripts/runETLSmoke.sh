#!/bin/bash

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

CONFIG_FOLDER=${WORKSPACE}/qa/env/mount/config

customer_classes=( "ImportRawToParquet" "ValidateRawData" "CreateCaseClass" "CleanseCaseClass" "CreateCompounds" )
hotlist_classes=( "ImportRawToParquet" "CreateCaseClass" "CleanseCaseClass" "CreateCompounds" )
transaction_classes=( "ImportRawToParquet" "ValidateRawData" "CreateCaseClass" "CleanseCaseClass" "CreateAggregationClass" "CreateCompounds" )

function createTestData() {

    echo "Creating test data"
    ./runQSSDataGenerator.sh -s com.quantexa.example.generators.AllRawDataGenerator -c ${CONFIG_FOLDER}/dataGenerationConfig.conf -r config

}

function etl() {

    classes=("$@")

    for d in ${documents[@]}; do
        for c in ${classes[@]}; do
            echo "Running $d:$c"
            if [ "$c" = "CleanseCaseClass" ]
            then
                bash ./runQSSWithLibpostal.sh $d -s com.quantexa.example.etl.projects.fiu.$d.$c \
		            -c ${CONFIG_FOLDER}/external.conf -r etl.$d || exit 1
		    elif [ "$c" = "CreateCompounds" ]
            then
                bash ./runQSS.sh $d -s com.quantexa.example.etl.projects.fiu.$d.$c \
		            -c ${CONFIG_FOLDER}/external.conf -r compounds.$d || exit 1
		    else
		        bash ./runQSS.sh $d -s com.quantexa.example.etl.projects.fiu.$d.$c \
                	-c ${CONFIG_FOLDER}/external.conf -r etl.$d || exit 1
		    fi
        done

        echo "Running $d:LoadElastic"
        bash ./runQSS.sh $d -s com.quantexa.example.etl.projects.fiu.$d.LoadElastic \
		    -c ${CONFIG_FOLDER}/external.conf -r elastic.$d || exit 1
    done
}

function etl_hotlist() {

    classes=("$@")

    for d in ${documents[@]}; do
        for c in ${classes[@]}; do
            echo "Running $d:$c"
            if [ "$c" = "CleanseCaseClass" ]
            then
                bash ./runQSSWithLibpostal.sh $d -s com.quantexa.example.etl.projects.fiu.$d.$c \
		            -c ${CONFIG_FOLDER}/external.conf -r etl || exit 1
		    elif [ "$c" = "CreateCompounds" ]
            then
                bash ./runQSS.sh $d -s com.quantexa.example.etl.projects.fiu.$d.$c \
		            -c ${CONFIG_FOLDER}/external.conf -r compounds.$d || exit 1
		    else
		        bash ./runQSS.sh $d -s com.quantexa.example.etl.projects.fiu.$d.$c \
                	-c ${CONFIG_FOLDER}/external.conf -r etl || exit 1
		    fi
        done

        echo "Running $d:LoadElastic"
        bash ./runQSS.sh $d -s com.quantexa.example.etl.projects.fiu.$d.LoadElastic \
		    -c ${CONFIG_FOLDER}/external.conf -r elastic.$d || exit 1
    done
}

function eng() {
    echo "Running ENGSpark"
    bash ./runENG.sh ${CONFIG_FOLDER}/resolver-config-fiu-smoke-eng.json \
	    ${CONFIG_FOLDER}/engSmoke.conf || exit 1
}

function loadEmptyResearchIndices() {
    echo "Running user defined intelligence script"
    bash ./runQSS.sh intelligence \
	    -s com.quantexa.example.etl.projects.fiu.intelligence.CreateElasticIndices \
	    -c ${CONFIG_FOLDER}/external.conf -r elastic.research || exit 1
}

createTestData
documents=("customer")
etl "${customer_classes[@]}"
documents=("hotlist")
etl_hotlist "${hotlist_classes[@]}"
documents=("transaction")
etl "${transaction_classes[@]}"
loadEmptyResearchIndices
eng
