#!/bin/bash

echo -e "nameserver 127.0.0.1\n$(cat /etc/resolv.conf)" | sudo tee /etc/resolv.conf
