#!/bin/bash
#
# Imports CA chain and connector cert into karaf keystore. Execute in $(Karaf-dir)/etc/keystores/
# Then, build karaf. The keystore is automatically deployed.
if [ $# -eq 1 ] ; then
  echo "Two arguments supplied, which is good"
else
  echo "Invalid number of arguments: $# (expected one)"
  echo "Usage: $0 PKCS12_filename (WITHOUT file ending)"
  exit 1
fi

#keytool -keystore $2_truststore -importcert -alias ca -file test_ca_certs/rootca.cert
#keytool -keystore $2_keystore -importcert -alias subca -file test_ca_certs/subca.cert
#keytool -keystore $1-truststore.jks -importcert -alias subca -file test_ca_certs/cachain.cert -noprompt
keytool -keystore $1-truststore.jks -importcert -alias ca -file test_ca_certs/rootca.cert
keytool -keystore $1-truststore.jks -importcert -alias subca -file test_ca_certs/subca.cert

keytool -v -importkeystore -srckeystore $1.p12 -srcstoretype PKCS12 -destkeystore $1-keystore.jks -deststoretype JKS

