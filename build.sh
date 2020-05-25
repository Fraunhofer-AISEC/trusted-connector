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

echo "In case of build errors, please verify that recent versions of docker and docker-compose are installed."
echo ""
# Pull is allowed to fail, ignore if it happens.
! docker-compose -f docker-build/docker-compose.yml pull
if [ -z "$*" ]; then
  docker-compose -f docker-build/docker-compose.yml run --rm build-container
else
  docker-compose -f docker-build/docker-compose.yml run --rm build-container "$*"
fi