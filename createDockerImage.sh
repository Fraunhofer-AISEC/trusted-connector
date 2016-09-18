#!/bin/sh

# Copy Karaf assembly to docker dir
rm -r docker/docker-karaf
mkdir docker/docker-karaf
cp -r karaf-assembly/target/assembly/* docker/docker-karaf/

cd docker
docker build . -t ids-core-platform