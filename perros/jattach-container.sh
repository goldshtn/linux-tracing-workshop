#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "USAGE: $0 <pid>"
    exit 1
fi

sudo nsenter -t $1 -m touch /proc/1/cwd/.attach_pid1
sudo kill -SIGQUIT $1

# The JVM should have responded by creating /tmp/.java_pid1, a UNIX domain
# socket that jattach can now attach to.

echo "Now run jattach 1 <command> [arguments]"
