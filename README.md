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
So the logical depedency graph (not the file system structure) is:<br />
.
├── _karaf-assembly //main module<br />
&nbsp;|   ├── _camel-ids<br />
&nbsp;|   ├── _camel-influxdb<br />
&nbsp;|   ├── _camel-multipart-processor<br />
&nbsp;|   ├── _ids-acme<br />
&nbsp;|   ├── _ids-api<br />
&nbsp;|   ├── _ids-comm<br />
&nbsp;|   ├── _ids-container-manager<br />
&nbsp;|   ├── _ids-dataflow-control<br />
&nbsp;|   ├── _ids-dynamic-tls<br />
&nbsp;|   ├── _ids-infomodel-manager<br />
&nbsp;|   ├── _ids-multipart-bean<br />
&nbsp;|   ├── _ids-route-manager<br />
&nbsp;|   ├── _ids-settings<br />
&nbsp;|   └── _ids-token-manager<br />
├── _ids-webconsole // webconsole can be run anywhere<br />
├── _jnr-unixsocket-wrapper //helper<br />
├── _karaf-features-ids //helper<br />
├── _rat_repository // can be run anywhere<br />
└── _tpm2j<br />
