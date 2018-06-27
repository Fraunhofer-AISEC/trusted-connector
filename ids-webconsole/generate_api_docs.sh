#!/bin/sh
docker run -it --user 1000 --name ids-api-docs --rm -p 4400:4400 -v ${PWD}/target/web/api/docs/api-docs/certs:/opt/public sourcey/spectacle /usr/local/bin/spectacle -d /opt/public/index.json
