#!/bin/bash
set -e

source $(dirname $0)/common.sh

pushd git-repo > /dev/null
./gradlew --no-daemon -PdeploymentRepository=${repository} build uploadArchives
popd > /dev/null
