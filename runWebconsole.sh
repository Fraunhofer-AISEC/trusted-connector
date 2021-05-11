#!/bin/bash
LAST_PWD=$(pwd)
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null && pwd )"

cd $DIR/ids-webconsole/src/main/angular
yarn run ng serve "$@"

cd $LAST_PWD
