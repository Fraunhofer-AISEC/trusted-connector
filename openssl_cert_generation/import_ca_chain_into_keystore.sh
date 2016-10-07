#!/bin/bash
#
# Imports CA chain and connector cert into karaf keystore. Execute in $(Karaf-dir)/etc/keystores/
# Then, build karaf. The keystore is automatically deployed.

keytool -keystore my-keystore.jks -storepass password -importcert -trustcacerts -noprompt -alias root -file rootca.cert
keytool -keystore my-keystore.jks -storepass password -importcert -alias ca -file subca.cert
keytool -keystore my-keystore.jks -storepass password -importcert -alias server -file connector.cert