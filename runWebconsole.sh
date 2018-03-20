#!/bin/bash
set LAST_PWD = $(pwd)

cd ~/VM_Shared/karaf-policy-platform/ids-webconsole/src/main/resources/www
ng serve --progress --delete-output-path=false "$@"

cd $LAST_PWD
