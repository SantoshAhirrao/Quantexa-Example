#!/bin/bash -x

set -e
exec >&2

echo compiling software ...

#export RANDOM_DATA_GENERATOR_SEED=6260565278463862333

version=$1
if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi	
cd $WORKSPACE

./gradlew clean example-ui:uberClean build slowTest sparkTest -x example-ui:compileJava -x example-ui:compileScala -x example-ui:nodeSetup -x example-ui:npmSetup -x example-ui:installNPM -x example-ui:runNPMBuild -x runNPMBuildLogin -x example-ui:build -x app-investigate:build -x app-search:build -x app-resolve:build -x app-graph-script:build -x app-security:build -x gateway:build -x config-service:build --max-workers=2 --refresh-dependencies --no-daemon --rerun-tasks -Dwoodhouse=true -Dorg.gradle.jvmargs="-Xms1g -Xmx8g"

#unset RANDOM_DATA_GENERATOR_SEED