#!/bin/bash

trap 'echo "" && echo "Error occurred, stopped process to prevent damage!" && exit 1' ERR

if [[ -z "$1" || -n "$2" ]]; then
  echo "Require connector release version as first and only parameter!"
  exit 1
fi

# start git flow release process
echo "Starting git flow release..."
git flow release start "$1"
# call version update script
./updateVersion.sh
# do a full clean install
echo "Performing a clean install..."
mvn clean install
# finish the feature
echo "Finish git flow release..."
git commit -am "Updated version to $1"
git flow release finish -k
