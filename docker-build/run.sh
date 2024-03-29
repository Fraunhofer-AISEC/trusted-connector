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

# Auto-dectect JAVA_HOME path
# shellcheck disable=SC2046
JAVA_HOME=$(dirname $(dirname $(readlink -f $(command -v javac))))
export JAVA_HOME

echo "Synchronize build-relevant files to /build (build volume)..."
rsync --exclude='/.git' --filter="dir-merge,- .dockerignore" -av --delete-after "$PROJECT_DIR/" /build/
echo ""

cd /build

# Get user UID of the /core-platfrom mount
TARGET_UID=$(stat -c %u "$PROJECT_DIR")
if [ "$TARGET_UID" == "root" ] || [ "$TARGET_UID" == "0" ]; then
  ln -s "$GRADLE_DIR" /root/.gradle
  ln -s "$M2_DIR" /root/.m2
  echo "Build parameters passed: $*"
  echo "User running ./gradlew: $(id)"
  # Run build using all arguments from CMD
  ./gradlew "$@"
  # Create target directory for results, see below
  mkdir -p "$PROJECT_DIR/ids-connector/build/libs"
else
  DOCKER_GID=$(stat -c %g /var/run/docker.sock)
  addgroup --gid "$DOCKER_GID" --quiet docker
  # Create a build user with the UID of the /core-platfrom mount
  adduser --uid "$TARGET_UID" --disabled-password --ingroup docker --gecos 'Build User' --quiet build
  # Fix ownerships for gradle and mvn
  chown -R "$TARGET_UID:$TARGET_UID" "$GRADLE_DIR" "$M2_DIR"
  # Create gradle/mvn symlinks in user directory
  ln -s "$GRADLE_DIR" /home/build/.gradle
  ln -s "$M2_DIR" /home/build/.m2
  echo "Build parameters passed: $*"
  # Run build using all arguments from CMD, passing correct HOME variable
  # Finally, create target directory for results with right owner
  sudo -u build -E sh -c "export HOME=\"/home/build\";
    echo \"User running ./gradlew: \$(id)\";
    ./gradlew $*;
    mkdir -p \"$PROJECT_DIR/ids-connector/build/libs\""
fi

echo "Synchronize built artifacts back to $PROJECT_DIR..."
rsync -cav --delete /build/ids-connector/build/libs/ "$PROJECT_DIR/ids-connector/build/libs/"
echo ""
