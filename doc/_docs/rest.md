---
layout: doc
title: Hosting Data Apps
permalink: /docs/rest/

---

In this tutorial you will set up a minimal scenario for retrieving data from a sensor and displaying it on a webpage, demonstrating different mechanisms of the trusted IDS connector:

##### Hosting Apps

The scenario includes two data apps: a __Provider App__ which retrieves sensor data from outside of the IDS and provides it as a REST service and a __Consumer App__ which retrieves data via REST and displays it in a webpage. The scenario shows how any application can be hosted by the Trusted Connector, as long as it is wrapped in a Docker or trustX container

* no matter which language it is programmed in
* no matter whether it has been written to run in an IDS connector or on any other platform. Apps do not have to be IDS-aware - in this scenario they use simple REST interfaces.


##### Remote Attestation with the IDS Protocol

Communication in this scenario runs over the secure IDS protocol. The IDS protocol is a subprotocol of WSS (Web Socket Security) and includes a remote attestation and meta data exchange when establishing a session between two endpoints. The remote attestation confirms that both endpoints are in a trusted state, i.e. that they run known and approved components.


##### Data Conversion

An (example) sensor provides measurement data via MQTT messages. A data app running in the __Provider Connector__ subscribes to MQTT messages and provides them via a REST interface. The connector retrieves it from the REST interface and sends it over the secure IDS protocol (IDSP) to the __Consumer Connector__. There, it is displayed in a web page. 

So, the conversion is: _MQTT -->  REST (text/plain) --> IDSP (binary blob) --> REST (text/plain) --> HTML_

## Download containers

Download the `docker-compose` description for Trusted Connector, Attestation Repository, TPM 2.0 emulator, and TPM daemon:


``` bash
$ curl -sS <some github URL>
```

## Start Trusted Connector

The following command will start the Core Platform container, a local attestation repository, a TPM 2.0 emulator, and a TPM daemon.


``` bash
$ docker-compose up
```

## Start Data Provider App

Start consuming temperature events from a global MQTT broker:

``` bash
$ node data-provider.js
```

## Start Data Consumer App

Start receiving sensor values via the IDS protocol and display them in a web page.

``` bash
$ node data-consumer.js
```

Open the web page at `http://localhost:8081`. You should see temperature values which have been received over the IDS protocol.