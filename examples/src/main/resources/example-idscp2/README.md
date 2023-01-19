# IDSCP2 communication examples

This examples demonstrate data transfer between trusted connectors via the IDSCP2 protocol.

### Default client-server-example

First, start the server with the command `docker compose --profile server up`.

Second, start the client using the command `docker compose --profile client up`.

### Broadcast example

For testing one-to-many broadcasting, the server and client can be started analogous to the client-server-example,
simply through adding `-f compose-broadcast.yaml` behind `docker compose`.
