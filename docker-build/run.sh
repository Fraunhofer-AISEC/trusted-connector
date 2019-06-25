#!/bin/bash

if [ ! -d "/core-platform" ]; then
    echo "Error: /core-platform not available, exiting..." >&2
    exit 1
fi
if [ ! -S "/var/run/docker.sock" ]; then
    echo "Error: /var/run/docker.sock not available, exiting..." >&2
    exit 1
fi
if [ ! -d "/.gradle" ]; then
    echo "Error: /.gradle not available, exiting..." >&2
    exit 3
fi
if [ ! -d "/.m2" ]; then
    echo "Error: /.m2 not available, exiting..." >&2
    exit 4
fi

cd /core-platform

# Remove the node user that could conflict with the UID needed for the build user
userdel node
# Create a build user with the UID of the /core-platfrom mount
TARGET_UID=$(ls -ld /core-platform | awk '{print $3}')
DOCKER_GID=$(ls -ld /var/run/docker.sock | awk '{print $4}')
addgroup --gid $DOCKER_GID --quiet docker
adduser --uid $TARGET_UID --disabled-password --ingroup docker --gecos 'Build User' --quiet build
ln -s /.gradle /home/build/.gradle
ln -s /.gradle /home/build/.m2

echo "Running ./gradlew with UID $TARGET_UID..."
# Run build using all arguments from CMD
#sudo -u build bash
sudo -u build ./gradlew "$@"