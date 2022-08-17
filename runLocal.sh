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

echo "This script starts a Trusted Connector locally."
echo "Productive instance are meant to run in containers, please use for TESTING ONLY!"
echo "It requires a successful build, by e.g. build.sh or fastBuild.sh."
echo "The working directory will be $(pwd). Deploy your Apache Camel routes to $(pwd)/deploy for testing."
if [ ! -d ./etc ]; then
  echo "./etc not found, try to copy etc from $(pwd)/examples..." >&2
  cp -r examples/etc .
fi
if [ ! -d ./deploy ]; then
  echo "./deploy not found, try to copy deploy from $(pwd)/examples..." >&2
  cp -r examples/deploy .
fi
echo ""

java --class-path "ids-connector/build/libs/libraryJars/*:ids-connector/build/libs/projectJars/*" "de.fhg.aisec.ids.TrustedConnector"