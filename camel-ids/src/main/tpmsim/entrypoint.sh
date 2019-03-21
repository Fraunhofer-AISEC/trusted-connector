#!/bin/sh

# Old, unixsocket-based communication method
# swtpm socket --tpmstate dir=/swtpm --tpm2 --ctrl type=unixio,path=/swtpm/swtpm-sock &

echo 'Starting swtpm (swtpm socket --tpmstate dir=/swtpm --tpm2 --server type=tcp,port=2321 --ctrl type=tcp,port=2322)...'
swtpm socket --tpmstate dir=/swtpm --tpm2 --server type=tcp,port=2321 --ctrl type=tcp,port=2322 &
sleep 1
echo 'Trying to execute "swtpm_ioctl -i --tcp localhost:2322"...'
until swtpm_ioctl -i --tcp localhost:2322; do sleep 1; done
echo 'Trying to execute "swtpm_bios --tpm2 --tcp localhost:2321"...'
until swtpm_bios --tpm2 --tcp localhost:2321; do sleep 1; done
echo 'Starting tpm2d...'
tpm2d -s