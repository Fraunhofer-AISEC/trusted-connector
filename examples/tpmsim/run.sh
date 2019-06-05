#!/bin/bash

RED='\033[0;31m'
NC='\033[0m'

# Initialize tpm2d data directory if not existing
if [ -z "$(ls /data)" ]; then
    mkdir -p /data/cml/tpm2d
fi
# Initialize TPM if not existing
if [ -z "$(ls /swtpm/*.permall 2>/dev/null)" ]; then
    echo "No .permall file found in /swtpm, initializing TPM..."
    apt-get update -qq
    apt-get install -qq gnutls-bin
    swtpm_setup --tpm2 --tpmstate /swtpm --create-ek-cert
    apt-get remove --purge gnutls-bin -qq
    apt-get autoremove --purge -qq
fi

# Old, unixsocket-based communication method
# swtpm socket --tpmstate dir=/swtpm --tpm2 --ctrl type=unixio,path=/swtpm/swtpm-sock &

echo 'Starting swtpm (swtpm socket --tpmstate dir=/swtpm --tpm2 --server type=tcp,port=2321 --ctrl type=tcp,port=2322)...'
swtpm socket --tpmstate dir=/swtpm --tpm2 --server type=tcp,port=2321 --ctrl type=tcp,port=2322 &
sleep 1
echo 'Trying to execute "swtpm_ioctl -i --tcp localhost:2322"...'
until swtpm_ioctl -i --tcp localhost:2322; do sleep 1; done
echo 'Trying to execute "swtpm_bios --tpm2 --tcp localhost:2321"...'
until swtpm_bios --tpm2 --tcp localhost:2321; do sleep 1; done

DEV_CERT=/data/cml/tokens/device.cert
if [ ! -f "$DEV_CERT" ]; then
    echo 'Starting tpm2d...'
    tpm2d -s &
    # Give tpm2d a little time to come up
    sleep 3
    echo "$DEV_CERT not found, running scd..."
    if [ ! -f /data/cml/device.conf ]; then
        echo "/data/cml/device.conf not found, initializing with random UUID..."
        echo -e "uuid: \"$(cat /proc/sys/kernel/random/uuid)\"" > /data/cml/device.conf
    fi
    scd && echo "scd executed successfully." &&
        echo "Replace device.cert with a certificate for device.csr signed of your PKI's CA and restart TPM simulator."
else
    echo 'Starting tpm2d...'
    tpm2d -s
fi