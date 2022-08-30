# IDSCP2 communication examples

This examples demonstrate data transfer between trusted connectors via the IDSCP2 protocol.

### Default client-server-example

First, start the server (consumer) with the command `docker-compose -f docker-compose-server.yaml up`.

Second, start the client (provider) using the command `docker-compose -f docker-compose-client.yaml up`.

### Broadcast example

For testing one-to-many broadcasting, the server and client can be started analogous to the client-server-example,
with the following command modifications:

- Write `docker-compose-broadcast-server.yaml` and `docker-compose-broadcast-client.yaml`
  instead of `docker-compose-server.yaml` and `docker-compose-client.yaml`, respectively.