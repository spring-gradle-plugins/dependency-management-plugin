#!/usr/bin/env sh

set -e

curl -L http://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip > gradle.zip
unzip gradle.zip
cd dependency-management-plugin
export GRADLE_OPTS=-Dorg.gradle.native=false
../gradle-$GRADLE_VERSION/bin/gradle -q build --stacktrace