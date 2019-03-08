[![Build Status](https://travis-ci.org/industrial-data-space/trusted-connector.svg?branch=develop)](https://travis-ci.org/industrial-data-space/trusted-connector)

The _Trusted Connector_ is an Apache Karaf-based platform for the Industrial Internet of Things (IIoT). It supports Docker and trust|me as containerization environments and provides the following features:

* Message routing and conversion between protocols with Apache Camel
* _Apps_ in isolated containers
* Data flow- and data usage control
* An Apache Camel component for secure communication and remote attestation between Connectors.

# How to build & run

Please see the [Github documentation page](https://industrial-data-space.github.io/trusted-connector-documentation/docs/dev_core/)

# How to contribute

Please refer to the [contribution guide](https://github.com/industrial-data-space/trusted-connector/blob/develop/.github/CONTRIBUTING.md)

# Project structure
So the logical depedency graph (not the file system structure) is:
.
├── _karaf-assembly //main module
|   ├── _camel-ids
|   ├── _camel-influxdb
|   ├── _camel-multipart-processor
|   ├── _ids-acme
|   ├── _ids-api
|   ├── _ids-comm
|   ├── _ids-container-manager
|   ├── _ids-dataflow-control
|   ├── _ids-dynamic-tls
|   ├── _ids-infomodel-manager
|   ├── _ids-multipart-bean
|   ├── _ids-route-manager
|   ├── _ids-settings
|   └── _ids-token-manager
├── _ids-webconsole // webconsole can be run anywhere
├── _jnr-unixsocket-wrapper //helper
├── _karaf-features-ids //helper
├── _rat_repository // can be run anywhere
└── _tpm2j
