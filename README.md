[![Build Status](https://travis-ci.org/industrial-data-space/trusted-connector.svg?branch=develop)](https://travis-ci.org/industrial-data-space/trusted-connector)

This project creates a custom Apaache Karaf assembly, including a custom feature "ids".

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


# How to build

## Dependencies

You need `docker`, `docker-compose` and `npm` installed.


## Run Maven

If you do not have access to Fraunhofer AISEC's internal docker registry, you will not be able to run integration tests nor wrap the Core Platform build in a container. In this case, append the `-Ddocker.skip` flag to your build command:

```
mvn clean install
```

You will now have a custom installation of Karaf including our own features in `karaf-assembly/target/assembly`


# How to run


## Variant 1: Run locally without docker

```
karaf-assembly/target/assembly/bin/karaf clean debug
```

(`clean` clears the workspace at startup, `debug` allows remote debugging. None of these is required)


If everything goes fine, you will see a Karaf shell. Type `help` to get started with it.





## Variant 2: Run in docker

If you have omitted the `-Ddocker.skip` flag for the build, you now have three docker containers:

- `registry.netsec.aisec.fraunhofer.de/ids/core-platform:latest` is the Core Platform container
- `registry.netsec.aisec.fraunhofer.de/ids/ttpsim:latest` is a trusted third party which owns a database of trusted integrity states
- `registry.netsec.aisec.fraunhofer.de/ids/tpm2dsim:latest` is the TPM 2.0 daemon which connects Core Platform to the TPM 2.0 simulator

You can run these three containers with a simple

```
docker-compose up
```



## URLs

When running, the following URLs will be available (note that `localhost` applies whenn runnning without docker. If you used `docker-compose up`, the hostname is the name of the Core Platform container):


`http://localhost:8181/`

IDS connector dashboard. This is the main UI of the connector and used for configuring the connector by the user.

`http://localhost:8181/cxf/api/v1/apps/list`

REST API endpoint for listing installed containers (dockers or trustme)

`http://localhost:8181/cxf/api/v1/apps/pull?imageId=<string>`

Pull and image and creates a container (but does not start it yet).

`http://localhost:8181/cxf/api/v1/apps/start?containerID=<string>`

Starts a container.

`http://localhost:8181/cxf/api/v1/apps/stop?containerId=<string>`

Stops a container.


`http://localhost:8181/cxf/api/v1/apps/wipe?containerId=<string>`

Removes a container (the image remains).

`http://localhost:8181/cxf/api/v1/config/list`


## Getting around in the Karaf shell

The Karaf shell is an adminstration environment for the Karaf platform. Try the following commands to check whether everything works:

`feature:list`: Shows all available features and their status (uninstalled or installed). Those marked with a star have explicitly been asked to be installed, other installed features are necessary dependencies. Make sure the `ids` feature is installed.

`bundle:list -t 0`: Lists all OSGi bundles. `-t 0` sets the start level threshold to 0 so you see all bundles. Otherwise you would only see bundles installed in addition the system bundles.

`camel:route-list`: Lists all active Camel routes

`log:tail`: Continuously displays the log. Press `Ctrl+C` to exit.

`log:display`: Display the log

`Ctrl+d`: Exits the Karaf shell




# Configuring routes

Create a file `karaf-assembly/target/assembly/deploy/my_route.xml` and paste the following lines into it:

```
<?xml version="1.0" encoding="UTF-8"?>
<blueprint
    xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="
      http://www.osgi.org/xmlns/blueprint/v1.0.0
      http://www.osgi.org/xmlns/blueprint/v1.0.0/blueprint.xsd">

    <camelContext xmlns="http://camel.apache.org/schema/blueprint">
      <route>
        <from uri="ids://echo"/>
        <log message="Copying ${file:name} to the output directory"/>
        <to uri="file:output"/>
      </route>
    </camelContext>

</blueprint>
```

If you observe the logs in the Karaf console with `log:tail` you will see how that route is picked up. Type `camel:route-list` to confirm the route has been picked up and activated. 

This route opens an endpoint speaking the IDS protocol (by default at port `9292`) and puts all data received at that endpoint into a file `karaf-assembly/target/assembly/output/<filename>`. 

# Setting up Eclipse

 <a href="http://repo1.maven.org/maven2/kr/motd/maven/os-maven-plugin/1.5.0.Final/os-maven-plugin-1.5.0.Final.jar">Download <code>os-maven-plugin-1.5.0.Final.jar</code></a> and put it into the <code>&lt;ECLIPSE_HOME&gt;/plugins</code> directory