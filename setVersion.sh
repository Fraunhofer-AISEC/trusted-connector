#!/bin/bash

trap 'echo "" && echo "Error occurred, stopped versioning process." && exit 1' ERR

if [[ -z "$1" || -n "$2" ]]; then
  echo "Require connector version as first and only parameter!"
  exit 1
fi

# read karaf version from pom.xml property
KARAF_VERSION=$(cat pom.xml | grep -oP "(?<=<karaf.version>)(.*)(?=</karaf.version>)")

# update version in maven projects
echo "Update versions in maven..."
mvn versions:set -DgenerateBackupPoms=false -DnewVersion="$1"
# update karaf shell branding
echo "Update shell branding..."
cat karaf-assembly/branding.properties.template | sed s/###CONNECTOR_VERSION###/$1/gm | sed s/###KARAF_VERSION###/$KARAF_VERSION/gm > karaf-assembly/src/main/resources/etc/branding.properties