# IDSCP2 communication examples

This examples demonstrate data transfer between trusted connectors via the IDSCP2 protocol.

### Default client-server-example

First, start the server (consumer) with the command `docker-compose -f docker-compose-server.yaml up`.
When the Karaf shell appears, you may attach to it via a separate SSH client connection using the command
`docker-compose -f docker-compose-server.yaml exec server-core bin/client`, and show logs output `log:tail`.

Second, start the client (provider) using the command `docker-compose -f docker-compose-provider.yaml up`.
When the Karaf shell appears, you may use `docker-compose -f docker-compose-provider.yaml exec client-core bin/client`
and `log:tail` analogous to the server.

### Broadcast example

For testing one-to-many broadcasting, the server and client can be started analogous to the client-server-example,
with the following command modifications:

- Write `docker-compose-broadcast-server.yaml` and `docker-compose-broadcast-client.yaml`
  instead of `docker-compose-server.yaml` and `docker-compose-client.yaml`, respectively.
- Container names for `exec` commands (`server-core` and `client-core`) switch places.