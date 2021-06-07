#!/bin/bash

trap finish INT TERM EXIT

finish() {
    rm -r ./runAll 2> /dev/null
    rm ./transferInstructions.sh 2> /dev/null
}

set -e

cd ..

if [ -z "$1" ]
then
    echo "No username provided. Example usage: ./transferToDevCluster joebloggs" && exit 1
fi

username=$1

if [ -z "$2" ]
then
    eval ./gradlew clean
    eval ./gradlew data-source-all:etl-all:build -x test
    eval ./gradlew example-scoring-batch:build --parallel -x test
    cd ./eng-spark-core-shadow
    eval ../gradlew build
    cd ..
fi

mkDir runAll
cd runAll
mkDir Jars
cd ..

cp ./example-scripts/runAllEtl.sh runAll
cp ./example-scripts/runSmokeWithVariables.sh runAll
cp ./example-scripts/runENG.sh runAll
cp ./example-scripts/runQSS.sh runAll
cp ./example-scripts/runQSSWithLibpostal.sh runAll
cp ./example-scripts/runScoring.sh runAll
cp ./example-config/engConfig.conf runAll
cp ./config/resolver-config-fiu-smoke.json runAll/resolver-config-fiu-smoke-eng.json
cp ./example-scoring/src/main/resources/parameterFile.csv runAll
cp ./data-source-all/etl-all/src/main/resources/environment-dev.conf runAll/external.conf

cp ./data-source-all/etl-all/build/libs/etl-all-shadow-*-all.jar runAll/Jars
cp ./example-scoring-batch/build/libs/example-scoring-batch-*-dependency.jar runAll/Jars
cp ./example-scoring-batch/build/libs/example-scoring-batch-*-projects.jar runAll/Jars
cp ./eng-spark-core-shadow/build/libs/quantexa-eng-spark-core-shadow_2.11-*-all.jar runAll/Jars

sed -i "s/username\=.*/username\=${username}/g" ./runAll/runSmokeWithVariables.sh
sed -i "s/root-hdfs-path \= .*/root-hdfs-path \= \"\/user\/${username}\/eng\/\"/g" ./runAll/engConfig.conf
sed -i "s/\"dataLocation\": \"C:\/dev\/app\/runQSS\/customer.*/\"dataLocation\": \"\/user\/${username}\/customer\/1\/Compounds\/DocumentIndexInput.parquet\",/g" ./runAll/resolver-config-fiu-smoke-eng.json
sed -i "s/\"dataLocation\": \"C:\/dev\/app\/runQSS\/hotlist.*/\"dataLocation\": \"\/user\/${username}\/hotlist\/Compounds\/DocumentIndexInput.parquet\",/g" ./runAll/resolver-config-fiu-smoke-eng.json
sed -i "s/\"dataLocation\": \"C:\/dev\/app\/runQSS\/transaction.*/\"dataLocation\": \"\/user\/${username}\/transaction\/1\/Compounds\/DocumentIndexInput.parquet\",/g" ./runAll/resolver-config-fiu-smoke-eng.json
sed -i "s/hdfsRoot=\/user\/quantexa/hdfsRoot=\/user\/quantexa\/${username}/g" ./runAll/runScoring.sh
sed -i '34,43d' ./runAll/resolver-config-fiu-smoke-eng.json

cp scripts/transferInstructionsTemplate.sh transferInstructions.sh
sed -i "s/cd \/quantexa\/users\/<USERNAME>/cd \/quantexa\/users\/${username}/g" ./transferInstructions.sh

psftp ${username}@10.17.255.1 -b ./transferInstructions.sh