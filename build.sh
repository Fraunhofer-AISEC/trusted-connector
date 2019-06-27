#!/bin/bash

echo "In case of build errors, please verify that recent versions of docker and docker-compose are installed."
echo ""
docker-compose -f docker-build/docker-compose.yml pull
docker-compose -f docker-build/docker-compose.yml run build-container