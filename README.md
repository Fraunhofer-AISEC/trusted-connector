[![Build Status](https://travis-ci.org/industrial-data-space/trusted-connector.svg?branch=develop)](https://travis-ci.org/industrial-data-space/trusted-connector)

The _Trusted Connector_ is an Apache Karaf-based platform for the Industrial Internet of Things (IIoT). It supports Docker and trust|me as containerization environments and provides the following features:

* Message routing and conversion between protocols with Apache Camel
* _Apps_ in isolated containers
* Data flow- and data usage control
* An Apache Camel component for secure communication and remote attestation between Connectors.

# How to run

## Start core platform
* Install docker-compose.
* Run core container, TPM2.0 simulator and simulator of trusted third party (ttpsim):
```
docker-compose up -d
```

## Open the dashboard

* Get IP address of the `iot-connector` container: 
```
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' iot-connector
```
* Open dashboard in browser: `http://<IP ADDRESS OF CONTAINER>:8181`.


# How to build

For detailed instruction on how to build the Trusted Connector with the trustme platform, please see the [Github documentation page](https://industrial-data-space.github.io/trusted-connector-documentation/docs/dev_core/)

## Subprojects

* `camel-ids`: An Apache Camel component implementing the "IDS Protocol" for remote attestation, meta data exchange, and message transfer between Trusted Connectors. The camel component supports the following protocol schemes: `idsserver://`, `idsclient://`, `idsclientplain://`
* `ids-dataflow-policy`: The LUCON data flow policy framework
* `ids-container-manager`: Container management service (cmld wrapper) for trustme and Docker containers
* `ids-route-manager`: Route management service for Apache Camel routes
* `ids-webconsole`: REST API and Angular2-based management web frontend
* `jnr-unixsocket`: Wrapper bundle for jnr-unixsockets and their dependencies. Allows Java to access UNIX domain sockets.
* `karaf-assembly`: Definition of the overall Karaf assembly (= the ready-to-run configured karaf application server)
* `karaf-feature-ids`: The "ids" feature including all bundles listed here.

Further, there are two additional sub-projects:

* `rat-repository`: A dummy remote attestation repository for testing purposes
* `tpm2j`: Java wrapper for TPM 2.0 daemon (tpmd)
