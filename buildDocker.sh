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

echo "This alternative build process creates the Trusted Connector core image"
echo "from a single 2-stage Dockerfile for the current architecture ONLY."
echo ""
echo "The build will be SLOWER and LESS EFFICIENTLY CACHED than builds using the buildx scripts!"
echo "For more efficient builds or multi-platform images, please have a look at buildx/docker-buildx.sh!"
echo ""
echo "Docker BuildKit will be enabled for more efficient (experimental) gradle/maven/nodejs caching."
echo ""

# Enable Docker BuildKit
export DOCKER_BUILDKIT=1

docker build . -t fraunhoferaisec/trusted-connector-core:develop