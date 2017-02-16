#!/bin/bash
#
# Copyright (C) 2013-2015 with regard to distribution / exploitation:
# Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
# 

cleanup(){
echo "Cleanup unnecessary files"
[[ -f ${CONNECTOR_CSR} ]] && rm ${CONNECTOR_CSR}
[[ -f ${CONNECTOR_CONFIG} ]] && rm ${CONNECTOR_CONFIG}
[[ -f ${INDEX_FILE} ]] && rm ${INDEX_FILE}
[[ -f ${SERIAL_FILE} ]] && rm ${SERIAL_FILE}
[[ -f ${INDEX_FILE}.attr ]] && rm ${INDEX_FILE}.attr
[[ -f ${INDEX_FILE}.old ]] && rm ${INDEX_FILE}.old
[[ -f ${SERIAL_FILE}.old ]] && rm ${SERIAL_FILE}.old
[[ -f 01.pem ]] && rm 01.pem
}

error_check(){
if [ "$1" != "0" ]; then
  echo "Error: $2"
  cleanup
  exit 1
fi
}

assert_file_exists(){
if [ ! -f $1 ]; then 
  echo "Error: Missing file $1"
  exit 1
fi
}

assert_file_not_exists(){
if [ -f $1 ]; then 
  echo "Error: File $1 exists. Precautional exit"
  exit 1
fi
}

# signing files
INDEX_FILE="certs/index.txt"
SERIAL_FILE="certs/serial.txt"

#CA files
SUBCA_CONFIG="configs/openssl-subca.cnf"
SUBCA_CERT="ca/subca.cert"
SUBCA_KEY="ca/subca.key"
CACHAIN_CERT="ca/cachain.cert"

#CONNECTOR certificate files
CONNECTOR_CONFIG="configs/openssl-connector.cnf"
CONNECTOR_CONFIG_TEMPLATE="configs/openssl-connector.cnf.template"
CONNECTOR_CSR="certs/$1.csr"
CONNECTOR_CERT="certs/$1.cert"
CONNECTOR_KEY="certs/$1.key"
CONNECTOR_P12="certs/$1.p12"

if [ $# -eq 1 ] ; then
  echo "One arguments supplied, which is good"
else
  echo "Invalid number of arguments: $# (expected 1)"
  echo "Usage: $0 filename"
  exit 1
fi

cd $(dirname $0)
echo "Check if directory is clean"
assert_file_not_exists ${INDEX_FILE}
assert_file_not_exists ${SERIAL_FILE}
assert_file_not_exists ${CONNECTOR_CSR}
assert_file_not_exists ${CONNECTOR_KEY}
assert_file_not_exists ${CONNECTOR_CERT}
assert_file_not_exists ${CONNECTOR_CONFIG}
assert_file_not_exists ${CONNECTOR_P12}

echo "Trying to find required files"
assert_file_exists ${SUBCA_KEY}
assert_file_exists ${SUBCA_CERT}
assert_file_exists ${SUBCA_CONFIG}
assert_file_exists ${CACHAIN_CERT}
assert_file_exists ${CONNECTOR_CONFIG_TEMPLATE}
echo "Successfully found requrired files"

########## CONNECTOR CERT ##########
echo "Create connector config"
# copy template and set random values to uuid&serial
cp ${CONNECTOR_CONFIG_TEMPLATE} ${CONNECTOR_CONFIG}

# create uuid and serial
UUID=$(cat /proc/sys/kernel/random/uuid)
SERIAL=$(cat /dev/urandom | tr -dc '0-9' | fold -w 16 | head -n 1)
sed -i "s/%%CONNECTOR_UUID%%/${UUID}/g" ${CONNECTOR_CONFIG}
sed -i "s/%%CONNECTOR_SERIAL%%/${SERIAL}/g" ${CONNECTOR_CONFIG}

echo "Create dummy connector CSR"
openssl req -batch -config ${CONNECTOR_CONFIG} -newkey rsa:2048 -sha256 -nodes -out ${CONNECTOR_CSR} -outform PEM -keyout certs/$1.key
error_check $? "Failed to create test connector CSR"

echo "Sign test connector CSR with sub CA certificate"
touch ${INDEX_FILE}
touch ${SERIAL_FILE}
echo '01' > ${SERIAL_FILE}
openssl ca -batch -config ${SUBCA_CONFIG} -policy signing_policy -extensions signing_req -out ${CONNECTOR_CERT} -infiles ${CONNECTOR_CSR}
error_check $? "Failed to sign connector CSR with CA certificate"

echo "Verify newly created test connector certificate"
openssl verify -CAfile ${CACHAIN_CERT} ${CONNECTOR_CERT}
error_check $? "Failed to verify newly signed test certificate"

echo "Create connector token with certifcate and key"
openssl pkcs12 -export -inkey ${CONNECTOR_KEY} -in ${CONNECTOR_CERT} -out ${CONNECTOR_P12}

echo "Test certificates successfully created"
cleanup

exit 0
