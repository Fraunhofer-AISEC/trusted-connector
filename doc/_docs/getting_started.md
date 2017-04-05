---
layout: doc
title: Getting Started
permalink: /docs/getting_started/

---

In this part you will learn how to download an instance of the Connector and start it on your computer.

> Note that this tutorial will use the (less secure) docker-based variant of the Connector. The alternative based on trustm3 comprises a whole OS stack, including the kernel.


## Prerequisites

The docker-based Connector requires any Linux operating system which supports docker. This is:

  - CentOS
  - Debian
  - Fedora
  - Oracle Linux
  - Red Hat Enterprise Linux
  - openSUSE and SUSE Linux Enterprise
  - Ubuntu
  - Gentoo
  - Arch
  - CRUX
  - Raspbian

MacOS is untested, but there are chances it will work.

> Note that Windows will not work (although it supports docker through virtualization) because we require UNIX domain socket communication.


## Install Docker

Install required packages

``` bash
$ sudo apt-get install apt-transport-https ca-certificates curl
```

Install PGP key of docker repository

``` bash
$ curl -fsSL https://yum.dockerproject.org/gpg | sudo apt-key add -
```

Add docker repository

``` bash
$ sudo add-apt-repository "deb https://apt.dockerproject.org/repo/ ubuntu-$(lsb_release -cs) main"
```

Load packages

``` bash
$ sudo apt-get update
```

Install docker

``` bash
$ sudo apt-get -y install docker-engine
```

## Install docker-compose

``` bash
$ curl -L "https://github.com/docker/compose/releases/download/1.10.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
```

``` bash
$ chmod +x /usr/local/bin/docker-compose
```

Confirm it works

``` bash
$ docker-compose --version
```

## Run a Local Connector Instance

Paste the following content into a file `docker-compose.yaml`. Please note that the docker registry used by that file requires you to log in before using `docker login ...` with your username and password.

``` yaml
version: '2'
services:

  # Image for TPM simulator
  ids-tpm2dsim:
    image: app-store.isst.fraunhofer.de:5000/ids/tpm2dsim
    volumes:
      - ./camel-ids/socket/:/data/cml/tpm2d/communication/
    command: /tpm2d/cml-tpm2d

  # Image for TPM trusted third party (RAT repository)
  ids-ttpsim:      
    image: app-store.isst.fraunhofer.de:5000/ids/ttpsim
    ports:
      - "31337:31337"

  # Image for core platform, gets docker control socket mounted into the image
  ids-core:
    image: app-store.isst.fraunhofer.de:5000/ids/core-platform
    volumes:
      - /tmp/ids/log/:/root/data/log/
      - /var/run/docker.sock:/var/run/docker.sock
      - ./camel-ids/socket/:/var/run/tpm2d/
    ports:
      - "5005:5005"
      - "9292:9292"
      - "8181:8181"

```

Start the connector:

```bash
$ docker-compose up
```

Congratulations, you just started your first connector! The webconsole of the connector is available at `http://<host>:8181/ids`.

## What did just happen?

You installed docker on your machine, downloaded and started a docker-compose definition. This pulled three different docker containers from a remote registry:

* IDS Core Container
* TPM 2.0 Simulator
* Remote Attestation Repository

The IDS Core Container is the heart of the trusted Connector. It is responsible for establishing communication to other connectors, routes messages between containers and configures the Connector.
The TPM 2.0 Simulator is a software trusted platform module which is responsible for attesting the integrity of the stack. In a _real_ setup, the TPM is a hardware module, but for the moment we keep it simple.