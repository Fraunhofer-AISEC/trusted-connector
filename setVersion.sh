#!/bin/bash

trap 'echo "" && echo "Error occurred, stopped versioning process." && exit 1' ERR

if [[ -z "$1" || -n "$2" ]]; then
  echo "Require connector version as first and only parameter!"
  exit 1
fi

# update version in maven projects
echo "Update versions in maven..."
mvn versions:set -DgenerateBackupPoms=false -DnewVersion="$1"