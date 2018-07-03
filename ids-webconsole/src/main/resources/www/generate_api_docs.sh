#!/bin/sh
echo "PWD ${PWD}"
docker run --user 1000 --name ids-api-docs --rm -p 4400:4400 -v ${PWD}/../../../../generated/swagger-ui:/opt/public sourcey/spectacle /usr/local/bin/spectacle /opt/public/swagger.json
