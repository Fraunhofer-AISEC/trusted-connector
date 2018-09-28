#!/bin/bash
LAST_PWD=$(pwd)
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null && pwd )"

cd $DIR/ids-webconsole/src/main/resources/www
ng serve --progress "$@"

cd $LAST_PWD
