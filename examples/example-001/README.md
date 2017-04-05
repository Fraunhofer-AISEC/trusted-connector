Example-001 demonstrates the connection between two REST services and the conversion of MQTT messages using the Trusted IoT Connector.

Start trusted third party (TTP) for remote attestation

```bash
docker-compose -f docker-compose-ttp.yaml up
```

Start data provider (subscribes to MQTT events and provides their content via REST)

```bash
docker-compose -f docker-compose-provider.yaml up
```

Start data consumer (receives messages over IDS protocol including TLS and remote attestation, forwards to internal REST app and displays data)

```bash
docker-compose -f docker-compose-consumer.yaml up
```

Open consumer app and see how temperature values are updated: http://172.21.0.3:8081


Open dashboards of consumer or provider: 

http://example001_provider-core_1:8181

http://example001_consumer-core_1:8282
