#!/bin/bash
set -e

source $(dirname $0)/common.sh

pushd git-repo > /dev/null
ulimit -n 65536
./gradlew --no-daemon -PdeploymentRepository=${repository} build uploadArchives
popd > /dev/null
