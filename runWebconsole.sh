#!/bin/bash
set LAST_PWD = $(pwd)
echo $%
cd ./ids-webconsole/src/main/resources/www
ng serve --progress --delete-output-path=false "$@"
cd $LAST_PWD
