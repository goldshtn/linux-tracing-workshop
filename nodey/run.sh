#!/bin/bash

export NODE_ENV=production

pkill node
if [ "$1" == "perf" ]; then
  node --perf_basic_prof bin/www >/dev/null &
elif [ "$1" == "prof" ]; then
  node --prof bin/www >/dev/null &
else
  node bin/www >/dev/null &
fi

sleep 1
curl -X POST 'http://localhost:3000/users/new?username=foo&password=pass'
curl -X POST 'http://localhost:3000/users/new?username=bar&password=pass'
