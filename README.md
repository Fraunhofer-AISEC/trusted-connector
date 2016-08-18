This project creates a custom Apaache Karaf configuration including a custom feature "camel-ids".

Apache Karaf is an OSGi runtime which has originally been the basis for the ServiceMix application service. Karaf has become independent from ServiceMix and is now a middleware which is commonly used in IoT platforms. Example projects using Karaf are the OpenHAB home automation server or the OpenDayLight SDN controller.

Karaf is merely a set of different "features". A feature is a deployment unit which provides a certain functionality. For example, the activemq message queue comes as an "activemq-broker" feature. This project defines a custom "assembly" of Karaf, i.e. instead of using the default features, we create our own configuration, which includes especially the ActiveMQ message queue and the Apache Camel routing engine.

In addition to standard Karaf features, we also create our own custom feature which is extends Camel with a new protocol endpoint "ids://". The actual code for this feature is in project "camel-ids" and the definition of the Karaf feature is in "ids-karaf-feature".

To build the project:

Built it:

```
mvn clean install
```

You will now have a custom installation of Karaf including our own features in `karaf-assembly/target/assembly`


Run it:

```
karaf-assembly/target/assembly/bin/karaf clean debug
```

(`clean` clears the workspace at startup, `debug` allows remote debugging. None of these is required)


If everything goes fine, you will see a Karaf shell. Type `help` to get started.
