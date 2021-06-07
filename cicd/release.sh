#!/bin/bash -x

exec >&2

VERSION=$1
NEW_VERSION=$2
DEPENDENCY_VERSION=$3
NEW_DEPENDENCY_VERSION=$4
INCUBATORS_DEPENDENCY_VERSION=$5
NEW_INCUBATORS_DEPENDENCY_VERSION=$6
RELEASE_BRANCH=$7
ENABLED=$8
IS_NIGHTLY_BUILD=$9
NIGHTLY_VERSION=${10}

function usage() {
    echo "usage: $0 version-to-release new-development-version"
    echo "eg. $0 1.0.0 1.0.1-SNAPSHOT"
}

if [[ "$IS_NIGHTLY_BUILD" == "true" ]]; then
    ./update_versions.sh ${IS_NIGHTLY_BUILD} ${NIGHTLY_VERSION}
    git diff --quiet && git diff --staged --quiet || git commit -a -m "Updating Q Dependency versions for nightly release for version: ${NIGHTLY_VERSION}" && git push --set-upstream origin $RELEASE_BRANCH
    exit 0
fi

if [[ -z "$ENABLED" ]] || [[ "$ENABLED" == "false" ]]; then
    exit 0
fi

if [[ $# -lt 2 ]]; then
    usage
    exit 1
fi

if [[ $(id -u) -ne 0 ]]; then
        echo "Script must be run as root"
        exit 1
fi

if [ -z "$WORKSPACE" ]
then
    WORKSPACE=`dirname $0`/..
fi
cd $WORKSPACE

git config --global user.email "jenkins@quantexa.com"
git config --global user.name "jenkins"

sed -i "s/quantexaLibraries.*/quantexaLibraries \= \"${DEPENDENCY_VERSION}\"/g" ./gradle/dependency-versions.gradle
sed -i "s/quantexaIncubators.*/quantexaIncubators \= \"${INCUBATORS_DEPENDENCY_VERSION}\"/g" ./gradle/dependency-versions.gradle

#Check for any changes to Q dependency and commit if any have been made
git diff --quiet && git diff --staged --quiet || git commit -a -m "Updating Q Dependency versions for release" && git push --set-upstream origin $RELEASE_BRANCH

./gradlew release -x test --max-workers=2 --no-daemon --rerun-tasks -Dorg.gradle.jvmargs=-Xmx8g -Prelease.useAutomaticVersion=true -Prelease.releaseVersion=$VERSION -Prelease.newVersion=$NEW_VERSION

sed -i "s/quantexaLibraries.*/quantexaLibraries \= \"${NEW_DEPENDENCY_VERSION}\"/g" ./gradle/dependency-versions.gradle
sed -i "s/quantexaIncubators.*/quantexaIncubators \= \"${NEW_INCUBATORS_DEPENDENCY_VERSION}\"/g" ./gradle/dependency-versions.gradle

#Check for any changes to Q dependency and commit if any have been made
git diff --quiet && git diff --staged --quiet || git commit -a -m "Updating Q Dependency versions post-release" && git push --set-upstream origin $RELEASE_BRANCH