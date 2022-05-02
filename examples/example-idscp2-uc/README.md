# IDSCP2 communication examples

This example demonstrates client-server communication between trusted connectors via the IDSCP2 protocol,
including LUCON UC (Usage Control), limiting execution to a particular docker container, identified by its content hash.

The example is started in the same way as described in `example-idscp2/README.md`, with the only difference that
container names in `exec` commands (`consumer-core` and `provider-core`) switch places.

If the UC demo fails, check whether the repo digest needs to be adapted in the XML file`example-idscp2-server.xml`
(last part of DockerHub URI). 
You can show the most recent digest using the command `docker images --digests jmalloc/echo-server`.