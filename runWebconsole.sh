#!/bin/bash
LAST_PWD=$(pwd)

cd ~/VM_Shared/karaf-policy-platform/ids-webconsole/src/main/resources/www
ng serve --progress "$@"

cd $LAST_PWD
