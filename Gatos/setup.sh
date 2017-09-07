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
# Taken from https://www.microsoft.com/net/core#linuxfedora
#
sudo rpm --import https://packages.microsoft.com/keys/microsoft.asc

sudo sh -c 'echo -e "[packages-microsoft-com-prod]\nname=packages-microsoft-com-prod \nbaseurl=https://packages.microsoft.com/yumrepos/microsoft-rhel7.3-prod\nenabled=1\ngpgcheck=1\ngpgkey=https://packages.microsoft.com/keys/microsoft.asc" > /etc/yum.repos.d/dotnetdev.repo'

sudo dnf update -y
sudo dnf install -y libunwind libicu
sudo dnf install -y dotnet-sdk-2.0.0

#
# LTTng
#
sudo dnf install -y lttng-tools
sudo dnf install -y babeltrace

#
# Taken from https://docs.docker.com/engine/installation/linux/docker-ce/fedora/#install-docker-ce
#
sudo dnf install -y dnf-plugins-core
sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo
sudo dnf makecache -y fast
sudo dnf install -y docker-ce
sudo systemctl start docker
