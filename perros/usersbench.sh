#!/usr/bin/bash

for user in `seq 1 50`; do
    begin=$(date +%s%N)
    curl -v "${1}/users?user=${user}" >/dev/null 2>&1
    end=$(date +%s%N)
    echo "user ${user} took $(((end-begin)/1000000)) ms"
done
