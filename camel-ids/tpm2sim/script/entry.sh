#!/bin/bash
USER_ID=$(stat -c "%u" /data/cml/communication/tpm2d)
echo "Starting with UID : $USER_ID"
useradd --shell /bin/bash -u $USER_ID -o -c "" -m tpm2d
export HOME=/tpm2d
chown -R tpm2d /tpm2d/
nohup runuser -l tpm2d -c /tpm2d/tpm2_simulator &
sleep 1
exec /usr/local/bin/gosu tpm2d "$@"
