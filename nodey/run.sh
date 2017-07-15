#!/bin/bash

export NODE_ENV=production

pkill node
if [ "$1" == "perf" ]; then
  FLAGS="--perf_basic_prof"
elif [ "$1" == "prof" ]; then
  FLAGS="--prof"
elif [ "$1" == "core" ]; then
  FLAGS="--abort-on-uncaught-exception"
else
  FLAGS=""
fi

node $FLAGS bin/www >/dev/null &

sleep 1
curl -X POST 'http://localhost:3000/users/new?username=foo&password=pass'
curl -X POST 'http://localhost:3000/users/new?username=bar&password=pass'
