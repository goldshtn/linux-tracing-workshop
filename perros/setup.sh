#!/usr/bin/bash
#
# Assumes most dependencies have already been installed on this image
# by the setup-fedora.sh script. Specifically, these labs depend on:
#   bcc
#   perf
#   perf-tools
#   FlameGraph
# And some miscellaneous utils: pidstat, curl, etc.

#
# Taken from https://docs.docker.com/engine/installation/linux/docker-ce/fedora/#install-docker-ce
#
sudo dnf install -y dnf-plugins-core
sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo
sudo dnf makecache -y fast
sudo dnf install -y docker-ce
sudo systemctl start docker
