#!/bin/sh
ps ax  | grep -i 'tpm2d.py' | grep -v grep | awk '{print $1}' | xargs kill -SIGTERM
