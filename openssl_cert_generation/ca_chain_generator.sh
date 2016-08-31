#!/bin/bash
#

cleanup(){
echo "Cleanup unnecessary files"
[[ -f ${SUBCA_CSR} ]] && rm ${SUBCA_CSR}
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

#root CA files
#root CA sign index file
INDEX_FILE="index.txt"
#root CA sign serial file
SERIAL_FILE="serial.txt"
#root CA config file
ROOTCA_CONFIG="test_configs/openssl-rootca.cnf"
#root CA cert file
ROOTCA_CERT="rootca.cert"
#root CA key file
ROOTCA_KEY="rootca.key"

#sub CA config file
SUBCA_CONFIG="test_configs/openssl-subca.cnf"
#sub CA csr file
SUBCA_CSR="subca.csr"
#sub CA cert file
SUBCA_CERT="subca.cert"
#sub CA key file
SUBCA_KEY="subca.key"

#ca chain file
CACHAIN_CERT="cachain.cert"

if [ $# -eq 0 ] ; then
  echo "No arguments supplied, which is good"
else
  echo "Invalid number of arguments: $# (expected 0)"
  echo "Usage: $0"
  exit 1
fi

cd $(dirname $0)
echo "Check if directory is clean"
assert_file_not_exists ${INDEX_FILE}
assert_file_not_exists ${SERIAL_FILE}
assert_file_not_exists ${ROOTCA_CERT}
assert_file_not_exists ${ROOTCA_KEY}
assert_file_not_exists ${SUBCA_CERT}
assert_file_not_exists ${SUBCA_CSR}
assert_file_not_exists ${SUBCA_KEY}
assert_file_not_exists ${CACHAIN_CERT}

echo "Trying to find required files"
assert_file_exists ${ROOTCA_CONFIG}
assert_file_exists ${SUBCA_CONFIG}
echo "Successfully found requrired files"


########## ROOT CA CERT ########## 
# -nodes option omits passphrase
echo "Create self-signed root CA certificate"
openssl req -batch -x509 -config ${ROOTCA_CONFIG} -days 7300 -newkey rsa:2048 -sha1 -nodes -out ${ROOTCA_CERT} -outform PEM
error_check $? "Failed to create self signed root CA certificate"

########## SUB CA CERT ########## 
# -nodes option omits passphrase
echo "Create sub CA CSR"
openssl req -batch -config ${SUBCA_CONFIG} -newkey rsa:4096 -sha1 -nodes -out ${SUBCA_CSR} -outform PEM
error_check $? "Failed to create sub CA CSR"

echo "Sign sub CA CSR with root CA"
touch ${INDEX_FILE}
touch ${SERIAL_FILE}
echo '01' > ${SERIAL_FILE}
openssl ca -batch -config ${ROOTCA_CONFIG} -policy signing_policy -extensions signing_req_CA -out ${SUBCA_CERT} -infiles ${SUBCA_CSR}
error_check $? "Failed to sign sub CA CSR with root CA certificate"

echo "Verify newly created sub CA certificate"
openssl verify -CAfile ${ROOTCA_CERT} ${SUBCA_CERT}
error_check $? "Failed to verify newly signed sub CA certificate"

echo "Concatenate root CA to sub CA"
cat ${ROOTCA_CERT} >> ${SUBCA_CERT} >> ${CACHAIN_CERT}

echo "Cleanup temp files"
cleanup

exit 0
