[![Build Status](https://travis-ci.org/industrial-data-space/trusted-connector.svg?branch=develop)](https://travis-ci.org/industrial-data-space/trusted-connector)

This project creates the _Core Platform_ of the Trusted Connector. It comes as a custom Apache Karaf assembly, including a feature `ids` which wraps the following subprojects:

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

Please see the [Github documentation page](https://industrial-data-space.github.io/trusted-connector-documentation/docs/dev_core/)


# Open the dashboard

When running, main UI of the connector if available under `http://localhost:8181`.

(If you run in a docker container, replace `localhost` by the name of the Core Platform container)


# Configuring routes

To set up a message route, create a file `karaf-assembly/target/assembly/deploy/my_route.xml` and paste the following lines into it:

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
