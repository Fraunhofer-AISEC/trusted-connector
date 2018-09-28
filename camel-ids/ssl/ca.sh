#!/bin/bash
#

cleanup(){
echo "Cleanup unnecessary files"
[[ -f ${SUBCA_CSR} ]] && rm ${SUBCA_CSR}
[[ -f ${INDEX_FILE}.attr ]] && rm ${INDEX_FILE}.attr
[[ -f ${INDEX_FILE}.old ]] && rm ${INDEX_FILE}.old
[[ -f ${SERIAL_FILE}.old ]] && rm ${SERIAL_FILE}.old
[[ -f ${PEM_FILE} ]] && rm ${PEM_FILE}
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

CA_FOLDER="./ca"

#root CA files
#root 01.pem
PEM_FILE="$CA_FOLDER/01.pem"
#root CA sign index file
INDEX_FILE="$CA_FOLDER/index.txt"
#root CA sign serial file
SERIAL_FILE="$CA_FOLDER/serial.txt"
#root CA config file
ROOTCA_CONFIG="configs/openssl-rootca.cnf"
#root CA cert file
ROOTCA_CERT="$CA_FOLDER/rootca.cert"
#root CA key file
ROOTCA_KEY="$CA_FOLDER/rootca.key"

#sub CA config file
SUBCA_CONFIG="configs/openssl-subca.cnf"
#sub CA csr file
SUBCA_CSR="$CA_FOLDER/subca.csr"
#sub CA cert file
SUBCA_CERT="$CA_FOLDER/subca.cert"
#sub CA key file
SUBCA_KEY="$CA_FOLDER/subca.key"

#ca chain file
CACHAIN_CERT="$CA_FOLDER/cachain.cert"

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
openssl req -batch -x509 -config ${ROOTCA_CONFIG} -newkey rsa:2048 -sha1 -nodes -out ${ROOTCA_CERT} -outform PEM -days 7300 -keyout ${ROOTCA_KEY}
error_check $? "Failed to create self signed root CA certificate"

########## SUB CA CERT ##########
# -nodes option omits passphrase
echo "Create sub CA CSR"
openssl req -batch -config ${SUBCA_CONFIG} -newkey rsa:2048 -sha1 -nodes -out ${SUBCA_CSR} -outform PEM -keyout ${SUBCA_KEY}
error_check $? "Failed to create sub CA CSR"

echo "Sign sub CA CSR with root CA"
touch ca/index.txt
touch ca/serial.txt
echo '01' > ca/serial.txt
openssl ca -batch -config ${ROOTCA_CONFIG} -policy signing_policy -extensions signing_req_CA -out ${SUBCA_CERT} -infiles ${SUBCA_CSR}
error_check $? "Failed to sign sub CA CSR with root CA certificate"

echo "Verify newly created sub CA certificate"
openssl verify -CAfile ${ROOTCA_CERT} ${SUBCA_CERT}
error_check $? "Failed to verify newly signed sub CA certificate"

echo "Concatenate root CA to sub CA"
echo "************************** ROOT CA CERTIFICATE *****************************************"
cat ${ROOTCA_CERT}
echo "************************** SUB CA CERTIFICATE *****************************************"
cat ${SUBCA_CERT}

cat ${ROOTCA_CERT} ${SUBCA_CERT} >> ${CACHAIN_CERT}

echo "Cleanup temp files"
cleanup

exit 0
