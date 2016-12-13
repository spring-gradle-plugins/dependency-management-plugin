#!/usr/bin/env sh

set -e

export GRADLE_OPTS=-Dorg.gradle.native=false
cd dependency-management-plugin
./gradlew -q build --stacktrace