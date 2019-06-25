#!/bin/bash
docker-compose -f docker-build/docker-compose.yml pull
docker-compose -f docker-build/docker-compose.yml run build-container yarnBuild install --parallel