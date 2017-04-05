#!/bin/sh
docker run -ti -P -v /var/run/docker.sock:/var/run/docker.sock -v /tmp/ids:/tmp ids-core-platform
