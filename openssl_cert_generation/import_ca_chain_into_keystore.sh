#!/bin/bash
#
# Imports CA chain and connector cert into karaf keystore. Execute in $(Karaf-dir)/etc/keystores/
# Then, build karaf. The keystore is automatically deployed.
if [ $# -eq 3 ] ; then
  echo "Three arguments supplied, which is good"
else
  echo "Invalid number of arguments: $# (expected 3)"
  echo "Usage: $0 PKCS12_filename ca_cert_filename keystore_filename"
  exit 1
fi
#keytool -keystore my-keystore.jks -storepass password -importcert -trustcacerts -noprompt -alias root -file rootca.cert
keytool -keystore $3 -importcert -alias ca -file $2
#keytool -keystore my-keystore.jks -storepass password -importcert -alias server -file connector.cert
keytool -v -importkeystore -srckeystore $1 -srcstoretype PKCS12 -destkeystore $3 -deststoretype JKS

