#!/bin/bash
USER_ID=${LOCAL_USER_ID:-9001}
echo "Starting with UID : $USER_ID"
useradd --shell /bin/bash -u $USER_ID -o -c "" -m tpm2d
export HOME=/tpm2d
chown -R tpm2d /tpm2d/
cd /tpm2d/
nohup runuser -l tpm2d -c /tpm2d/tpm2_simulator &
sleep 1
exec /usr/local/bin/gosu tpm2d "$@"
