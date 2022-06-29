#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

pushd git-repo > /dev/null
ulimit -n 65536
./gradlew --no-daemon -PdeploymentRepository=${repository} build publish
popd > /dev/null
