#!/bin/sh
docker build -t fraunhoferaisec/iot-connector-core-platform:develop karaf-assembly/target/docker/registry.netsec.aisec.fraunhofer.de/ids/core-platform/develop/build/
docker push fraunhoferaisec/iot-connector-core-platform:develop
docker build -t fraunhoferaisec/tpm2dsim:develop camel-ids/target/docker/
docker push fraunhoferaisec/tpm2dsim:develop
docker build -t fraunhoferaisec/ttpsim:develop rat-repository/target/docker/
docker push fraunhoferaisec/ttpsim:develop
docker build -t fraunhoferaisec/example-client:develop examples/example-009/ids-example-009-client/
docker push fraunhoferaisec/example-client:develop
docker build -t fraunhoferaisec/example-server:develop examples/example-009/ids-example-009-server/
docker push fraunhoferaisec/example-server:develop
