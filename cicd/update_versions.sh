#!/bin/bash -x

exec >&2

IS_NIGHTLY_BUILD=$1
DATE_VERSION=$2

#DATE_VERSION will be of the form: nightly-dd-MM-yy

if [[ "$IS_NIGHTLY_BUILD" == "true" ]] ; then
    # We need to update the dependency versions for quantexaLibraries, quantexaIncubators and eng after each nightly build
    # We need to replace either "quantxaLibraries = A.B.C" (immediately after a release)
    # or "quantexaLibraries = X.Y.Z-nightly-dd-MM-yy"(after a previous daily build) with "quantexaLibraries = X.Y.Z-nightly-dd+1-MM-yy"
    # We can get dd+1-MM-yy from our JenkinsFile, but we need to find out what X.Y.Z is
    # We can find it by looking at the version in the gradle.properties file

    # First grep the first line in the gradle.properies file which contains the word version e.g. version=X.Y.Z-SNAPSHOT
    versionLine=$(grep "version" ../gradle.properties)

    #Then remove anything before the equals sign eg X.Y.Z-SNAPSHOT
    versionLineSplit=${versionLine#*=}

    #Next remove anything after a - eg X.Y.Z
    version=${versionLineSplit%-*}

    #Finally feed the version into a sed command updating the dependency version for each of the Quantexa dependencies
    sed -i "s/quantexaLibraries \= .*/quantexaLibraries \= \"${version}-${DATE_VERSION}\"/g" ../gradle/dependency-versions.gradle
    sed -i "s/quantexaIncubators \= .*/quantexaIncubators \= \"${version}-${DATE_VERSION}\"/g" ../gradle/dependency-versions.gradle
    sed -i "s/version\=.*/version\=${version}-${DATE_VERSION}/g" ../eng-spark-core-shadow/gradle.properties
fi