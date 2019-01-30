This is an extension of the default example for communication over localhost (or any other test network).

It relies on the docker0 bridge using the gateway IP 172.17.0.1 (default value for docker setups).

The following lines have been added to the provider YAML file (consumer remains unchanged):
```
provider-core:
  [...]
  extra_hosts:
    - "consumer-core:172.17.0.1"
```
Due to this directive, the provider will connect to localhost over bridge0,
instead of contacting the consumer directly via the `ids-wide` bridge.

The IP address can be adjusted to another address of a machine where the example consumer is running.