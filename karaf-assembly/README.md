The _karaf-assembly_ is the main module, containing the karaf instance. This creates the binary that launches the OSGi engine. All other modules run on top of this. 


Create a custom Apache Karaf configuration including Camel.

Built it:

```
mvn clean install
```


Run it:

```
target/assembly/bin/karaf clean debug
```

(`clean` clears the workspace at startup, `debug` allows remote debugging. None of these is required)
