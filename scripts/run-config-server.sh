#!/usr/bin/env bash

cd ..

SCRIPT=`realpath $0`
SCRIPTPATH=`dirname $SCRIPT`

echo $SCRIPTPATH

export VERSION=""

for i in $(grep -i "^version" gradle.properties | cut -d= -f2)
do
  export VERSION=$i
done

export REPOSITORY_ROOT=${SCRIPTPATH}

#export REPOSITORY_ROOT=%REPOSITORY_ROOT:\=/%

export CONFIG_DIR=${REPOSITORY_ROOT}/config

cd ${REPOSITORY_ROOT}/config-service/build/libs

java -Dspring.cloud.config.server.native.searchLocations=file:///${CONFIG_DIR} -Dspring.cloud.config.server.overrides.config-files.dir=file:///${CONFIG_DIR} -Dspring.cloud.config.server.overrides.quantexa.licence.path=${CONFIG_DIR}/quantexa.licence -jar config-service-${VERSION}.jar
# FOR BASIC SECURITY ADD  -Dsecurity.user.name=config -Dsecurity.user.password=verysecure -Dencrypt.key=foobarbaz
# TO CHANGE GIT REPO LOCATION ADD  -Dspring.cloud.config.server.git.uri=${CD}\config-repo