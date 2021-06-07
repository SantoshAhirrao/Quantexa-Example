#!/bin/bash -x

set -e
exec >&2

echo deploying to Kubernetes ...

VERSION=$1

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi
cd $WORKSPACE

./gradlew helmDelete \
--max-workers=2 \
--no-daemon \
--rerun-tasks \
-Ptiller_namespace=jenkins-accelerators-tiller \
-Phelm_executable=/usr/local/bin/helm \
-Phosting_cloud_provider=google \
-Phosting_environment=qcp-core-green-1 \
-Phelm_chart_name=QuantexaExplorer \
-Dorg.gradle.jvmargs="-Xmx2g"

#sleep 3m

helm init --client-only --tiller-namespace=jenkins-accelerators-tiller

#SECRET_FILE_USR and SECRET_FILE_PSW are set as environment variables by the "Run Helm" stage in the Jenkinsfile

./gradlew helmInstallOrUpgrade \
--max-workers=2 \
--no-daemon \
--rerun-tasks \
--stacktrace \
-Ptiller_namespace=jenkins-accelerators-tiller \
-Pjenkins_username=$SECRET_FILE_USR \
-Pjenkins_password=$SECRET_FILE_PSW \
-Phelm_executable=/usr/local/bin/helm \
-Phosting_cloud_provider=google \
-Phosting_environment=qcp-core-green-1 \
-Phelm_chart_name=QuantexaExplorer \
-Dorg.gradle.jvmargs="-Xmx2g"
