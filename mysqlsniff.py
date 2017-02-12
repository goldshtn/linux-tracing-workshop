#!/usr/bin/env python
#
# mysqlsniff    MySQL client command sniffer tool.
#               This tool is experimental and designed for teaching purposes
#               only; it is neither tested nor suitable for production work.
#
# Copyright (C) Sasha Goldshtein, 2017

import argparse
import ctypes as ct
import sys
from bcc import BPF

parser = argparse.ArgumentParser(description="MySQL client command sniffer " +
    "tool. Probes the network send layer and attempts to detect MySQL " +
    "commands.")
parser.add_argument("-p", "--pid", type=int, required=True,
                    help="the client process id")
parser.add_argument("-f", "--filter", type=str, default="",
                    help="the query prefix to search for in commands")
parser.add_argument("-S", "--stack", action="store_true",
                    help="capture a stack trace for each command")
parser.add_argument("-d", "--debug", action="store_true",
                    help="show raw message contents for debugging purposes")
parser.add_argument("-l", "--library", default="pthread",
                    help="the library to probe (default: pthread)")
parser.add_argument("-a", "--api", default="__libc_send",
                    help="the API to probe (default: __libc_send)")
args = parser.parse_args()

text = """
#include <linux/ptrace.h>
#include <linux/socket.h>

struct data_t {
    char data[128];
    int len;
#ifdef NEED_STACK
    int stackid;
#endif
};

BPF_PERF_OUTPUT(events);
BPF_STACK_TRACE(stacks, 1024);

static inline bool starts_with(char *query) {
    char needle[] = "%s";""" % args.filter + """
    #pragma unroll
    for (int i = 0; i < sizeof(needle)-1; ++i) {
        if (query[i] != needle[i])
            return false;
    }
    return true;
}

int probe(struct pt_regs *ctx, int fd, const void *buf, size_t len) {
    struct data_t data = {0};
    bpf_probe_read(&data.data[0], sizeof(data.data), (void *)buf);

    int cmd_len = *(int *)&data.data[0];
    u8 cmd = data.data[4];
    if (cmd != 3)
        return 0;   // not a query

    if (!starts_with(data.data + 5))
        return false;

    data.len = cmd_len + 5;
#ifdef NEED_STACK
    data.stackid = stacks.get_stackid(ctx,
                                      BPF_F_REUSE_STACKID | BPF_F_USER_STACK);
#endif
    events.perf_submit(ctx, &data, sizeof(data));

    return 0;
}
"""

if args.stack:
    text = "#define NEED_STACK\n" + text

class Data(ct.Structure):
    _fields_ = [
        ("data", ct.c_ubyte * 128),
        ("len", ct.c_int),
        ("stackid", ct.c_int)
    ]

def fmt_char(c):
    return unichr(c) if c > 20 and c < 128 else '.'

bpf = BPF(text=text)
# This is by far the most brittle part of the script. We need to attach
# The MySQL Java client uses __libc_send in libpthread, while the Node.js
# MySQL module uses __write in libpthread. In other scenarios, this will
# likely need to change. The user can control these values using cmdline args.
bpf.attach_uprobe(name=args.library, sym=args.api,
                  fn_name="probe", pid=args.pid)
def print_event(cpu, data, size):
    event = ct.cast(data, ct.POINTER(Data)).contents
    data = event.data[:event.len]
    if args.debug:
        print(''.join(fmt_char(x) for x in data))
        print(' '.join('{:02x}'.format(x) for x in data))
    else:
        print(''.join(fmt_char(x) for x in data[5:-1]))
    if args.stack:
        for addr in stacks.walk(event.stackid):
            print("\t%s" % bpf.sym(addr, args.pid, show_offset=True))
        print("")

bpf["events"].open_perf_buffer(print_event)
if args.stack:
    stacks = bpf["stacks"]
print("Sniffing process %d, Ctrl+C to quit." % args.pid)
while 1:
    bpf.kprobe_poll()
