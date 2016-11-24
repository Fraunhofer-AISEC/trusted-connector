#!/bin/sh
nohup runuser -l tpm2d -c /tpm2d/tpm2_simulator &
sleep 1
runuser -l tpm2d -c /tpm2d/cml-tpm2d & 
/ttp/ttp.py 
