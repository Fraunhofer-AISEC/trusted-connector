#!/bin/bash

PLATFORM=${1:-$(uname -i || (echo "Platform detection failed, please provide platform as 1st parameter!" && exit))}
EXAMPLE_TAG=${2:-"develop"}
BASE_IMAGE=${3:-"debian:buster-slim"}

echo "Fetching project version from ./gradlew..."
PROJECT_VERSION=$(docker-compose -f docker-build/docker-compose.yml run --rm build-container \
  properties --no-daemon --console=plain -q | grep "^version:" | awk '{gsub(/[ \t\n\r]+$/,""); print $2}')
printf "######################################################################\n"
printf "Detected project version: %s\n" "$PROJECT_VERSION"
printf "Using build tag \"%s\" and base image \"%s\"\n" "$EXAMPLE_TAG" "$BASE_IMAGE"
printf "######################################################################\n\n"
echo "Building core container for $PLATFORM as fraunhoferaisec/trusted-connector-core:$EXAMPLE_TAG..."
docker buildx build --build-arg "PLATFORM=$PLATFORM" --build-arg "BASE_IMAGE=$BASE_IMAGE" karaf-assembly \
  --load -t "fraunhoferaisec/trusted-connector-core:$EXAMPLE_TAG"
echo "Building TPM simulator for $PLATFORM as fraunhoferaisec/tpmsim:$EXAMPLE_TAG..."
docker buildx build --build-arg "PLATFORM=$PLATFORM" examples/tpmsim --load -t "fraunhoferaisec/tpmsim:$EXAMPLE_TAG"
echo "Building TTP (trusted third party) simulator for $PLATFORM as fraunhoferaisec/ttpsim:$EXAMPLE_TAG..."
docker buildx build --build-arg "PLATFORM=$PLATFORM" --build-arg "BASE_IMAGE=$BASE_IMAGE" \
  --build-arg "VERSION=$PROJECT_VERSION" rat-repository --load -t "fraunhoferaisec/ttpsim:$EXAMPLE_TAG"
echo "Building IDSCP example client/server for $PLATFORM as fraunhoferaisec/example-[client/server]:$EXAMPLE_TAG..."
docker buildx build --build-arg "PLATFORM=$PLATFORM" examples/example-idscp/example-client \
  --load -t "fraunhoferaisec/example-client:$EXAMPLE_TAG"
docker buildx build --build-arg "PLATFORM=$PLATFORM" examples/example-idscp/example-server \
  --load -t "fraunhoferaisec/example-server:$EXAMPLE_TAG"

