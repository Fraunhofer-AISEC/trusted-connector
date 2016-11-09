#!/bin/sh
rm -rf docker-files
cp -r ../karaf-assembly/target/assembly src/main/docker/docker-files && \
docker build -t ids/core-platform .
