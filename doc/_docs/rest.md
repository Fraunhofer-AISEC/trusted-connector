---
layout: doc
title: Connecting REST Apps
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

## Unzip Example Setup

As member of the Industrial Data Space association, you have received a Zip file `trusted-connector-examples_0.1` containing all necessary files for this example. Unzip the file in some folder and go to the contained folder `example-001`.

This folder contains docker-compose descriptions for three main entities: the _Provider Connector_, the _Consumer Connector_, and the _Trusted Third Party_. In a production setup, these three entities would be remotely connected and operated by different owners. For the sake of this example, we will run them on your local machine.


## Start Trusted Third Party

Start the Trusted Third Party (TTP) which holds trusted PCR values for remote attestation.

``` bash
$ docker-compose -f docker-compose-ttp.yaml up
```

## Start Provider

Start the __Provider Connector__. It includes a simple node app which subscribes to temperature values via MQTT and provides them as REST service. A message route will connect to this REST interface, receive the values and push them to the __Consumer Connector__ over the IDS protocol. The _Provider Connector_ also includes a TPM daemon and a TPM 2.0 simulator for remote attestation.

``` bash
$ docker-compose -f docker-compose-provider.yaml up
```

## Start Consumer

Start the __Consumer Connector__. It receives sensor values over the secured and remotely attested IDS protocol and forwards it to a simple node app running in the __Consumer Connector__. The node app receives data via HTTP POST requests and displays it in a web page.

``` bash
$ docker-compose -f docker-compose-consumer.yaml up
```

Open the web page at `http://example001_consumer-app_1:8081`. You should see temperature values which have been received over the IDS protocol.