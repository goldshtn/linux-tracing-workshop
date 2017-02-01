#!/usr/bin/env python
#
# Example BPF program for aggregating outgoing network interface traffic
# using the net:net_dev_* kernel tracepoints and displaying a histogram.
#
# Copyright 2016 Sasha Goldshtein

from time import sleep
from bcc import BPF

text = """
#include <linux/netdevice.h>

struct key_t {
    char devname[16]; // IFNAMSIZ
    u64 slot;
};

BPF_HISTOGRAM(dist, struct key_t);

TRACEPOINT_PROBE(net, net_dev_start_xmit) {
    struct key_t key = {0};

    TP_DATA_LOC_READ_CONST(&key.devname, name, 16);
    key.slot = bpf_log2l(args->len);
    dist.increment(key);

    return 0;
}
"""

bpf = BPF(text=text)
dist = bpf["dist"]
while True:
    sleep(1)
    dist.print_log2_hist("bytes", "device name")
