#!/bin/bash

set -o errexit -o pipefail -o noclobber -o nounset

# Save working directory
OLD_PWD="$PWD"
# Return to saved working directory on error/exit
trap_handler() {
  cd "$OLD_PWD" || exit 9
}
trap "trap_handler" ERR EXIT INT TERM

cd "$(dirname "${BASH_SOURCE[0]}")" || return

echo "Warning: fastBuild.sh requires JDK 11+, node 12.22+ incl. npm and protoc (i.e. protobuf-compiler) to be installed locally on your machine."
echo "Lacking any of these dependencies will cause this build to fail."
echo "For a pre-configured build environment, use build.sh, which requires only docker and docker-compose."
echo ""
./gradlew yarnBuild :ids-connector:build --parallel