This is a script collection for creating a very simple Root/Sub-CA for Industrial Dataspace Test Environments.
** This is not meant for real world scenarios. Certificates should be issued by a real CA and handled accordingly. **

To create the CA/Sub-CA:
```
./ca.sh
```

To create a certificate for a device:
```
./cert.sh "connector-name"
```
Before creating a device certificate, make sure the configs/openssl-connector.cnf.template template file is adapted to the use case. Please change the DNS/IP Adresses needed to your setup. 


To create a keystore and truststore:
```
./keystore.sh "connector-name"
```


Documentation is located at https://fraunhofer-aisec.github.io/trusted-iot-connector-documentation/

(C)) Fraunhofer AISEC 2018
