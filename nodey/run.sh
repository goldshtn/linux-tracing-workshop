#!/bin/bash

export NODE_ENV=production

function kill_ns {
  if ps -ef | grep -v grep | grep -q -F './slodns'; then
    ps -ef | grep './slodns' | grep -v grep | awk '{ print $2 }' | \
             xargs sudo kill -9
  fi
}

function setup_ns {
  if ! grep -q -F 127.0.0.1 /etc/resolv.conf; then
    echo -e "nameserver 127.0.0.1\n$(cat /etc/resolv.conf)" \
            | sudo tee /etc/resolv.conf
  fi
  kill_ns
  if ! [ -f ./slodns ]; then
    wget -q https://raw.githubusercontent.com/goldshtn/slodns/master/slodns \
         -O slodns
    chmod u+x ./slodns
  fi
  sudo ./slodns -p 53 -d 1000 -j 500 >/dev/null &
  disown
}

function bench {
  for x in `seq 0 50`; do
    for y in `seq 0 50`; do
      start_ts=`date +%s%N`
      curl -X POST "http://localhost:3000/position?x=$x&y=$y&z=0"
      end_ts=`date +%s%N`
      runtime=$(((end_ts-start_ts)/1000))
      printf "running time: %dus\n" $runtime
    done
  done
}

if [ "$1" == "perf" ]; then
  FLAGS="--perf_basic_prof"
elif [ "$1" == "prof" ]; then
  FLAGS="--prof"
elif [ "$1" == "core" ]; then
  FLAGS="--abort-on-uncaught-exception"
elif [ "$1" == "dns" ]; then
  setup_ns
elif [ "$1" == "killdns" ]; then
  kill_ns
  exit
elif [ "$1" == "bench" ]; then
  bench
  exit
else
  FLAGS="$@"
fi

pkill node
node $FLAGS bin/www >/dev/null &

sleep 1
curl -X POST 'http://localhost:3000/users/new?username=foo&password=pass'
curl -X POST 'http://localhost:3000/users/new?username=bar&password=pass'
