#!/bin/sh
USER_ID=$(stat -c "%u" /data/cml/tpm2d/communication/)
echo "Starting with UID : $USER_ID"
useradd --shell /bin/sh -u $USER_ID -o -c "" -m tpm2d
export HOME=/tpm2d
chown -R tpm2d /tpm2d/
chown -R tpm2d /data/
nohup su -l tpm2d -c /tpm2d/tpm2_simulator &
sleep 2
exec /sbin/su-exec tpm2d "$@"
