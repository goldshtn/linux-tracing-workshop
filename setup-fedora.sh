#!/bin/bash
#
# Configure a lab environment for Linux tracing workshop, with exercises
# using perf, flame graphs, and the BPF tool collection. Requires a system
# with Fedora 24/25, and a recent Linux kernel (4.6+).
#
# Copyright: Sasha Goldshtein, 2017
#
# Distributed under the MIT license (see the LICENSE file in this directory).

function die {
	echo >&2 "$@"
	exit 1
}

function upgrade_kernel {
    read -p "Your kernel is too old. Upgrade to latest mainline? [y/N] " -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    else
        curl -s https://repos.fedorapeople.org/repos/thl/kernel-vanilla.repo | sudo tee /etc/yum.repos.d/kernel-vanilla.repo
        sudo dnf --enablerepo=kernel-vanilla-mainline update -y
        echo "Reboot into the new kernel and then try this script again."
        exit 1
    fi
}

### Make sure curl is installed, we need it very early on:
echo "Installing curl..."
sudo dnf install -y curl

### Check BPF kernel config flags
echo "Checking BPF config flags..."
for flag in CONFIG_BPF CONFIG_BPF_SYSCALL CONFIG_BPF_JIT CONFIG_BPF_EVENTS; do
    sysver=$(uname -r)
    present=`sudo cat /boot/config-$sysver | grep $flag= | cut -d= -f2`
    [[ "$present" = "y" ]] || die "$flag must be set"
done

### Check for supported Fedora versions
echo "Checking if this version of Linux is supported..."
(uname -r | grep "fc2[45]" -q) || \
    die "Unsupported Linux version, only Fedora 24/25 is currently supported"

### Check for kernel version
echo "Checking if this version of the kernel is supported..."
[[ $(uname -r) =~ ^([0-9]+)\.([0-9]+) ]]
majver=${BASH_REMATCH[1]}
minver=${BASH_REMATCH[2]}
if [[ "$majver" -lt "4" ]]
    then upgrade_kernel
fi
if [[ "$majver" -eq "4" && "$minver" -lt "6" ]]
    then upgrade_kernel
fi

### Install perf and kernel headers from vanilla repo
echo "Installing perf and kernel headers..."
sudo dnf --enablerepo=kernel-vanilla-mainline install -y perf
sudo dnf --enablerepo=kernel-vanilla-mainline --best --allowerasing \
     install -y kernel-devel kernel-headers

### Install basics
echo "Installing basics..."
sudo dnf install -y wget git ncurses-devel sysstat atop httpd-tools file \
                    lldb bind-utils bc gnuplot
sudo dnf install -y vim

### Install glibc debuginfo
echo "Installing glibc debuginfo..."
sudo dnf debuginfo-install -y glibc

### Create root directory for all the tools
INSTALL_ROOT=~/tracing-workshop
mkdir -p $INSTALL_ROOT || die "Unable to create installation directory"
pushd $INSTALL_ROOT

### Clone required GitHub repos
echo "Cloning GitHub repos to build from source..."
git clone --depth=1 https://github.com/goldshtn/linux-tracing-workshop labs
git clone https://github.com/iovisor/bcc
git clone --depth=1 https://github.com/brendangregg/FlameGraph
git clone --depth=1 https://github.com/nodejs/node
git clone --depth=1 https://github.com/postgres/postgres
git clone --depth=1 https://github.com/MariaDB/server mariadb
git clone --depth=1 https://github.com/jrudolph/perf-map-agent
git clone --depth=1 https://github.com/brendangregg/perf-tools
git clone --depth=1 https://github.com/goldshtn/slodns

### Install prerequisites for building stuff
echo "Installing build tools..."
sudo dnf install -y systemtap-sdt-devel
sudo dnf install -y bison cmake ethtool flex git iperf libstdc++-static \
  python-netaddr python-pip gcc gcc-c++ make zlib-devel \
  elfutils-libelf-devel gnutls-devel redhat-rpm-config python-devel
sudo dnf install -y clang clang-devel llvm llvm-devel llvm-static
sudo dnf install -y luajit luajit-devel
sudo pip install pyroute2

NUMPROCS=$(nproc --all)

### Build BCC from source
echo "Building BCC from source..."
mkdir bcc/build; pushd bcc/build
cmake .. -DCMAKE_INSTALL_PREFIX=/usr
make -j $NUMPROCS
echo "Installing into /usr/share/bcc..."
sudo make install
popd

### Put perf-tools in /usr
echo "Installing perf-tools into /usr/share/perf-tools..."
sudo mkdir -p /usr/share/perf-tools
sudo cp -R ./perf-tools/bin /usr/share/perf-tools

### Install OpenJDK
echo "Installing OpenJDK..."
sudo dnf install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel
echo "Installing OpenJDK debuginfo..."
sudo dnf debuginfo-install -y java-1.8.0-openjdk

### Building perf-map-agent
echo "Building perf-map-agent..."
pushd perf-map-agent
cmake .
make
bin/create-links-in .
popd

### Build Node from source
echo "Building Node from source..."
pushd node
./configure --with-dtrace --enable-d8
make -j $NUMPROCS
sudo make install
popd

### Download required Node modules
echo "Installing required Node modules..."
npm install llnode
npm install stackvis
npm install 0x

### Build Postgres from source
echo "Building Postgres from source..."
pushd postgres
./configure --enable-dtrace --without-readline
make -j $NUMPROCS
sudo make install
popd

### Build MariaDB from source
echo "Building MariaDB from source..."
pushd mariadb
cmake . -DENABLE_DTRACE=1
make -j $NUMPROCS
sudo make install
popd

### Install MySQL Python connector
echo "Installing MySQL Python connector..."
sudo PATH=/usr/local/mysql/bin:$PATH pip install mysql-python

### Setting up Postgres
echo "Setting up Postgres with user 'postgres'..."
sudo adduser postgres
sudo mkdir /usr/local/pgsql/data
sudo chown postgres /usr/local/pgsql/data
sudo -u postgres /usr/local/pgsql/bin/initdb -D /usr/local/pgsql/data
echo "To start Postgres, run 'sudo -u postgres /usr/local/pgsql/bin/postgres -D /usr/local/pgsql/data >logfile 2>&1 &'"

### Setting up MySQL
echo "Setting up MySQL with user 'mysql'..."
sudo groupadd mysql
sudo useradd -g mysql mysql
pushd /usr/local/mysql
sudo chown -R mysql .
sudo chgrp -R mysql .
sudo scripts/mysql_install_db --user=mysql
sudo chown -R root .
sudo chown -R mysql data
echo "To start MySQL, run 'sudo -u mysql /usr/local/mysql/bin/mysqld_safe --user=mysql &'"
popd

### Setting environment variables
echo "Setting environment variables for PATH and MANPATH..."
sudo bash -c 'cat >> /etc/profile << \EOF
  PATH=$PATH:/usr/share/bcc/tools:/usr/share/perf-tools
  MANPATH=$MANPATH:/usr/share/bcc/man/man8
EOF'

popd
