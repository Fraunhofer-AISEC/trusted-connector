#!/bin/bash
#
# Imports CA chain and connector cert into karaf keystore. Execute in $(Karaf-dir)/etc/keystores/
# Then, build karaf. The keystore is automatically deployed.

keytool -keystore keystore.jks -storepass password -importcert -trustcacerts -noprompt -alias root -file ../../../../../ssl/ca/rootca.cert
keytool -keystore keystore.jks -storepass password -importcert -alias ca -file ../../../../../ssl/ca/subca.cert
keytool -keystore keystore.jks -storepass password -importcert -alias client -file ../../../../../ssl/certs/client.cert
keytool -keystore keystore.jks -storepass password -importcert -alias server -file ../../../../../ssl/certs/server.cert

