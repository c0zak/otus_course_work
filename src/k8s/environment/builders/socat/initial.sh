#!/bin/sh

backend=$(cat /config/socat.conf)
command="socat -ddd $backend"
exec $command
