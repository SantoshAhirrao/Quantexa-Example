#!/bin/bash -x

set -e
exec >&2

echo compiling software ...

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi	
cd $WORKSPACE

cd eng-spark-core-shadow
../gradlew clean build --max-workers=2 --info -Dorg.gradle.jvmargs=-Xmx8g
cd ..

./gradlew example-graph-scripting:graph-script-batch:projectShadowJar \
example-graph-scripting:graph-script-batch:dependencyShadowJar \
data-source-all:etl-all:shadowJar \
example-scoring-batch:projectShadowJar \
example-scoring-batch:dependencyShadowJar \
example-data-generator:projectShadowJar \
example-data-generator:dependencyShadowJar \
example-task-loading:task-loading-batch:dependencyShadowJar \
example-task-loading:task-loading-batch:projectShadowJar

echo "assembling libpostal ..."

cd /jpostal
gradle assemble

mkdir /quantexa/libpostal_1.0/joint
cp /usr/local/lib/libpostal.so.1 /quantexa/libpostal_1.0/joint
cp /jpostal/src/main/jniLibs/libjpostal_expander.so /quantexa/libpostal_1.0/joint
cp /jpostal/src/main/jniLibs/libjpostal_parser.so /quantexa/libpostal_1.0/joint




