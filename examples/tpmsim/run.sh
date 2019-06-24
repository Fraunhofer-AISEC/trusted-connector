#!/bin/bash

RED='\033[0;31m'
NC='\033[0m'

# Extract tpm2d/swtpm state archive, if existing
if [ -f "/tpmsim_data.tar" ]; then
    if [ -d "/data" ] || [ -d "/swtpm" ]; then
        echo "Error: /data and/or /swtpm and tpmsim_data.tar exist." >&2
        echo "Either provide tpmsim_data.tar or those volumes! Aborting..." >&2
        exit 1
    fi
    tar -xf /tpmsim_data.tar
else
    # Initialize tpm2d data directory if not existing
    if [ ! -d "/data/cml/tpm2d" ]; then
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

# Set environment for openssl_tpm2_engine to use simulator
export TPM_INTERFACE_TYPE=socsim
export TPM_SERVER_TYPE=raw

DEV_CERT=/data/cml/tokens/device.cert
if [ ! -f "$DEV_CERT" ]; then
    echo 'Starting tpm2d...'
    tpm2d -s &
    # Give tpm2d a little time to come up
    sleep 5
    echo "$DEV_CERT not found, running scd..."
    # Create /dev/tpm0 to workaround a sanity check for a real tpm device in scd
    touch /dev/tpm0
    if [ ! -f /data/cml/device.conf ]; then
        echo "/data/cml/device.conf not found, initializing with random UUID..."
        echo -e "uuid: \"$(cat /proc/sys/kernel/random/uuid)\"" > /data/cml/device.conf
    fi
    n=0
    until [ $n == 3 ]
    do
        scd && break  # substitute your command here
        echo "Provisioning failed, possibly due to TPM_RC_RETRY. Retrying..."
        n=$[$n+1]
    done
    if [ $n == 3 ]; then
        echo "scd execution failed $n times, aborting..."
    else
        echo "scd executed successfully."
        echo "Replace device.cert with a certificate for device.csr signed of your PKI's CA and restart TPM simulator."
    fi
else
    echo 'Starting tpm2d...'
    tpm2d -s
fi