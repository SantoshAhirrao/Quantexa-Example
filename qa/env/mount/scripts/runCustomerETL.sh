#!/bin/bash

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi

CONFIG_FOLDER=${WORKSPACE}/qa/env/mount/config

customer_classes=( "ImportRawToParquet" "ValidateRawData" "CreateCaseClass" "CleanseCaseClass" "CreateCompounds" )

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

createTestData
documents=("customer")
etl "${customer_classes[@]}"
