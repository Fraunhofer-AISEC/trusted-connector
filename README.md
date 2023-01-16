![build](https://github.com/industrial-data-space/trusted-connector/workflows/build/badge.svg)

The _Trusted Connector_ is a Spring Boot-based platform for the Industrial Internet of Things (IIoT). It supports Docker and trust|me as containerization environments and provides the following features:

* Message routing and conversion between protocols with Apache Camel
* _Apps_ in isolated containers
* Data flow- and data usage control
* An Apache Camel component for secure communication and remote attestation between Connectors.

The _Trusted Connector_ has acquired the IDS_ready label. _Trusted Connector_ is a composite of the Core Container and the overall system. Please see the online documentation for details. 

![IDS_ready](https://github.com/industrial-data-space/trusted-connector-documentation/blob/master/docs/assets/img/IDS-ready-component.jpg?raw=true)

# How to build & run

Please see the [Github documentation page](https://industrial-data-space.github.io/trusted-connector-documentation/docs/dev_core/)

# How to contribute

Please refer to the [contribution guide](https://github.com/industrial-data-space/trusted-connector/blob/develop/.github/CONTRIBUTING.md)

# Project structure

├── __camel-influxdb__ Influx DB adapter for Apache Camel. (optional. It is not included in the assembly by default)<br />
├── __camel-processors__ _Apache Camel Processors for IDS Multipart, contract negotiation and other IDS-specific message types_<br />
├── __ids-acme__ _ACME 2 client for retrieving TLS certificates for the web console UI_<br />
├── __ids-api__ _Internal APIs of all IDS connector modules._<br />
├── __ids-connector__ _Central subproject for TC Spring Boot build and configurations_<br />
├── __ids-container-manager__ _Management interface to the underlying container management layer (trustme or docker)_<br />
├── __ids-dataflow-control__ _LUCON data flow policy framework_<br />
├── __ids-infomodel-manager__ _Provides the IDS information model_<br />
├── __ids-route-manager__ _Management interface to the underlying message router (Apache Camel)<br />
├── __ids-settings__ _Manages connector configuration_<br />
├── __ids-webconsole__ _Management UI for the connector. Is contained in default assembly but could be moved out of it, if a smaller code base is desired_<br />