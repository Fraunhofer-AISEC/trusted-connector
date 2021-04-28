![build](https://github.com/industrial-data-space/trusted-connector/workflows/build/badge.svg)

> :warning: Note: We are currently in the transition of OSGi to Spring Boot. You can track the current progress in [this](https://github.com/industrial-data-space/trusted-connector/issues/49) issue. If you are looking for a stable version, please use the latest OSGi-based release ([4.0.0](https://github.com/industrial-data-space/trusted-connector/releases/tag/4.0.0)) or the [master](https://github.com/industrial-data-space/trusted-connector/tree/master) branch.

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

├── __camel-influxdb__ Influx DB adapter for Apache Camel. (optional. It is not included in the assembly by default)<br />
├── __camel-multipart-processor__ _REST/MultiPart protocol as an Apache Camel component_<br />
├── __ids-acme__ _ACME 2 client for retrieving TLS certificates for the web console UI_<br />
├── __ids-api__ _Internal APIs of all IDS connector modules._<br />
├── __ids-container-manager__ _Management interface to the underlying container management layer (trustme or docker)_<br />
├── __ids-dataflow-control__ _LUCON data flow policy framework_<br />
├── __ids-dynamic-tls__ _Fragment bundle to allow refreshing TLS certificates in Jetty web server without restarting_<br />
├── __ids-infomodel-manager__ _Provides the IDS information model_<br />
├── __ids-route-manager__ _Management interface to the underlying message router (Apache Camel)<br />
├── __ids-settings__ _Manages connector configuration_<br />
├── __ids-token-manager__ _Acquires and verifies JWT tokens received from the DAPS server_<br />
├── __ids-webconsole__ _Management UI for the connector. Is contained in default assembly but could be moved out of it, if a smaller code base is desired_<br />
├── __karaf-features-ids__ _Feature definition for Apache Karaf runtime_<br />
