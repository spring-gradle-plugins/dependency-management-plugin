#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/deployment-repository

pushd git-repo > /dev/null
./gradlew --no-daemon -PdeploymentRepository=${repository} build publish
popd > /dev/null
