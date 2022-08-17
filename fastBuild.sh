#!/bin/bash

echo "Warning: fastBuild.sh requires JDK 8, node>=10;<12, npm and yarn to be installed locally on your machine."
echo "Lacking any of these dependencies will make this build fail."
echo "For a pre-configured build environment, use build.sh, which requires only docker and docker-compose."
echo ""
./gradlew yarnBuild install --parallel