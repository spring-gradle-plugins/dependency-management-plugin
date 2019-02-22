#!/bin/bash
set -e

version=$( cat version/version )
milestone=${version}

java -jar /github-release-notes-generator.jar \
  --releasenotes.github.username=${GITHUB_USERNAME} \
  --releasenotes.github.password=${GITHUB_TOKEN} \
  --releasenotes.github.organization=spring-gradle-plugins \
  --releasenotes.github.repository=dependency-management-plugin  \
  ${milestone} generated-release-notes/release-notes.md

echo ${version} > generated-release-notes/version
echo v${version} > generated-release-notes/tag
