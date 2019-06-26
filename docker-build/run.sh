#!/bin/bash

if [ ! -d "$PROJECT_DIR" ]; then
    echo "Error: $PROJECT_DIR not available, exiting..." >&2
    exit 1
fi
if [ ! -S "/var/run/docker.sock" ]; then
    echo "Error: /var/run/docker.sock not available, exiting..." >&2
    exit 2
fi
if [ ! -d "$GRADLE_DIR" ]; then
    echo "Error: $GRADLE_DIR not available, exiting..." >&2
    exit 3
fi
if [ ! -d "$M2_DIR" ]; then
    echo "Error: $M2_DIR not available, exiting..." >&2
    exit 4
fi

cd $PROJECT_DIR

# Remove the node user that could conflict with the UID needed for the build user
userdel node
# Create a build user with the UID of the /core-platfrom mount
TARGET_UID=$(ls -ld $PROJECT_DIR | awk '{print $3}')
if [ $TARGET_UID == "root" ]; then
    ln -s $GRADLE_DIR /root/.gradle
    ln -s $M2_DIR /root/.m2
    echo "Running ./gradlew as root..."
    echo "Build parameters passed: $@"
    # Run build using all arguments from CMD
    ./gradlew "$@"
else
    DOCKER_GID=$(ls -ld /var/run/docker.sock | awk '{print $4}')
    addgroup --gid $DOCKER_GID --quiet docker
    adduser --uid $TARGET_UID --disabled-password --ingroup docker --gecos 'Build User' --quiet build
    ln -s $GRADLE_DIR /home/build/.gradle
    ln -s $M2_DIR /home/build/.m2
    echo "Running ./gradlew with UID $TARGET_UID..."
    echo "Build parameters passed: $@"
    # Run build using all arguments from CMD
    sudo -u build ./gradlew "$@"
fi