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
