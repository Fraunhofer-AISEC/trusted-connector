#!/bin/bash
(
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" > /dev/null && pwd )"

cd $DIR/ids-webconsole/src/main/angular
npx yarn run ng serve "$@"
)

