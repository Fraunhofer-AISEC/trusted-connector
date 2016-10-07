#!/bin/bash
#
# Imports CA chain and connector cert into karaf keystore. Execute in $(Karaf-dir)/etc/keystores/
# Then, build karaf. The keystore is automatically deployed.

keytool -keystore keystore.jks -storepass password -importcert -trustcacerts -noprompt -alias root -file ../../../../../openssl_cert_generation/test_certificates/rootca.cert
keytool -keystore keystore.jks -storepass password -importcert -alias ca -file ../../../../../openssl_cert_generation/test_certificates/subca.cert
keytool -keystore keystore.jks -storepass password -importcert -alias server -file ../../../../../openssl_cert_generation/test_certificates/connector.cert