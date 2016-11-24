#!/bin/sh
su -m tpm2d -c 'nohup /tpm2d/tpm2_simulator' &
sleep 1
su -m tpm2d -c /tpm2d/cml-tpm2d 
 
