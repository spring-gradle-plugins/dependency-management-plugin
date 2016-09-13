#!/usr/bin/env sh

set -e

curl --silent --output gradle.zip -L http://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip
unzip -qq gradle.zip
cd dependency-management-plugin
export GRADLE_OPTS=-Dorg.gradle.native=false
../gradle-$GRADLE_VERSION/bin/gradle -q build --stacktrace