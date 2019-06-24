This is an docker-compose override file for example-idscp featuring communication over localhost (or any other test network).

It relies on the docker0 bridge using the gateway IP 172.17.0.1 (default value for docker setups).

Usage (assumes example-idscp as working directory):
`docker-compose -f docker-compose-provider.yaml -f ../example-idscp-localhost/docker-compose-provider.override.yaml up`

The following lines are overridden in the provider YAML file (consumer remains unchanged):
```
provider-core:
  extra_hosts:
    - "consumer-core:172.17.0.1"
```
Due to this directive, the provider will connect to localhost over bridge0,
instead of contacting the consumer directly via the `ids-wide` network.

The IP address can be adjusted to another address of a machine where an example consumer is running.