#!/usr/bin/env python
#
# nhttpslower   Snoops and prints Node.js HTTP requests slower than a threshold.
#               This tool is experimental and designed for teaching purposes
#               only; it is neither tested nor suitable for production work.
#
# NOTE: Node http__client* probes are not accurate in that they do not report
#       the fd, remote, and port. This makes it impossible to correlate request
#       and response probes, and a result the tool does not work correctly if
#       there are multiple client requests in flight in parallel.
#
# Copyright (C) Sasha Goldshtein, 2017

import argparse
import ctypes as ct
import sys
from bcc import BPF, USDT, DEBUG_PREPROCESSOR

# TODO This can also be done by using uprobes -- explore what it would take
# TODO Filter by HTTP method, or by URL prefix/substring

parser = argparse.ArgumentParser(description="Snoops and prints Node.js HTTP " +
    "requests and responses (client and server) slower than a threshold. " +
    "Requires Node.js built with --enable-dtrace (confirm by using tplist).")
parser.add_argument("pid", type=int, help="the Node.js process id")
parser.add_argument("threshold", type=int, nargs="?", default=0,
                    help="print only requests slower than this threshold (ms)")
parser.add_argument("-S", "--stack", action="store_true",
                    help="capture a stack trace for each event")
parser.add_argument("--client", action="store_true",
                    help="client events only (outgoing requests)")
parser.add_argument("--server", action="store_true",
                    help="server events only (incoming requests)")
parser.add_argument("-d", "--debug", action="store_true",
                    help="print the BPF program (for debugging purposes)")
args = parser.parse_args()

text = """
#include <uapi/linux/ptrace.h>

struct val_t {
    u64 start_ns;
    u64 duration_ns;
    int type;       // CLIENT, SERVER
#ifdef NEED_STACK
    int stackid;
#endif
    char method[16];
    char url[128];
};

struct data_t {
    u64 start_ns;
    u64 duration_ns;
    int port;
    int type;
    int stackid;
    char method[16];
    char url[128];
    char remote[128];
};

struct key_t {
    // TODO nhttpsnoop correlates by remote and port, while we use fd and port
    int fd;
    int port;
};

#define CLIENT 0
#define SERVER 1

BPF_PERF_OUTPUT(events);
BPF_STACK_TRACE(stacks, 1024);
BPF_HASH(starts, struct key_t, struct val_t);

#define THRESHOLD """ + str(args.threshold*1000000) + "\n"

probe = """
int PROBE_start(struct pt_regs *ctx) {
    struct val_t val = {0};
    struct key_t key = {0};
    char *str;

    bpf_usdt_readarg(7, ctx, &key.fd);
    bpf_usdt_readarg(4, ctx, &key.port);
    bpf_usdt_readarg(5, ctx, &str);
    bpf_probe_read(val.method, sizeof(val.method), str);
    bpf_usdt_readarg(6, ctx, &str);
    bpf_probe_read(val.url, sizeof(val.url), str);

    val.start_ns = bpf_ktime_get_ns();
    val.type = TYPE;
#ifdef NEED_STACK
    val.stackid = stacks.get_stackid(ctx,
                                     BPF_F_REUSE_STACKID | BPF_F_USER_STACK);
#endif

    starts.update(&key, &val);
    return 0;
}

int PROBE_end(struct pt_regs *ctx) {
    struct key_t key = {0};
    struct val_t *valp;
    struct data_t data = {0};
    u64 duration;
    char *remote;

    bpf_usdt_readarg(4, ctx, &key.fd);
    bpf_usdt_readarg(3, ctx, &key.port);

    valp = starts.lookup(&key);
    if (!valp)
        return 0;   // Missed the start event for this request

    duration = bpf_ktime_get_ns() - valp->start_ns;
    if (duration < THRESHOLD)
        goto EXIT;

    data.start_ns = valp->start_ns;
    data.duration_ns = duration;
    data.port = key.port;
    data.type = valp->type;
#ifdef NEED_STACK
    data.stackid = valp->stackid;
#endif
    __builtin_memcpy(data.method, valp->method, sizeof(data.method));
    __builtin_memcpy(data.url, valp->url, sizeof(data.url));

    bpf_usdt_readarg(2, ctx, &remote);
    bpf_probe_read(&data.remote, sizeof(data.remote), remote);

    events.perf_submit(ctx, &data, sizeof(data));

EXIT:
    starts.delete(&key);
    return 0;
}
"""

if not args.server:
    text += probe.replace("PROBE", "client").replace("TYPE", "CLIENT")
if not args.client:
    text += probe.replace("PROBE", "server").replace("TYPE", "SERVER")

CLIENT = 0
SERVER = 1

if args.stack:
    text = "#define NEED_STACK\n" + text

usdt = USDT(pid=args.pid)
if not args.server:
    usdt.enable_probe("http__client__request", "client_start")
    usdt.enable_probe("http__client__response", "client_end")
if not args.client:
    usdt.enable_probe("http__server__request", "server_start")
    usdt.enable_probe("http__server__response", "server_end")
bpf = BPF(text=text, usdt_contexts=[usdt],
          debug=DEBUG_PREPROCESSOR if args.debug else 0)

class Data(ct.Structure):
    _fields_ = [
        ("start_ns", ct.c_ulong),
        ("duration_ns", ct.c_ulong),
        ("port", ct.c_int),
        ("type", ct.c_int),
        ("stackid", ct.c_int),
        ("method", ct.c_char * 16),
        ("url", ct.c_char * 128),
        ("remote", ct.c_char * 128)
    ]

delta = 0
prev_ts = 0

def print_event(cpu, data, size):
    global delta, prev_ts
    event = ct.cast(data, ct.POINTER(Data)).contents
    typ = "CLI" if event.type == CLIENT else "SVR"
    if prev_ts != 0:
        delta += event.start_ns - prev_ts
    prev_ts = event.start_ns
    print("%-14.9f %3s %11.3f %-16s %-5d %-8s %s" %
          (delta/1e9, typ, event.duration_ns/1e6, event.remote,
           event.port, event.method, event.url))
    if args.stack:
        for addr in stacks.walk(event.stackid):
            print("\t%s" % bpf.sym(addr, args.pid, show_offset=True))
        print("")

bpf["events"].open_perf_buffer(print_event)
if args.stack:
    stacks = bpf["stacks"]
print("Snooping HTTP requests in Node process %d, Ctrl+C to quit." % args.pid)
print("%-14s %3s %-11s %-16s %-5s %-8s %s" %
      ("TIME_s", "TYP", "DURATION_ms", "REMOTE", "PORT", "METHOD", "URL"))
while 1:
    bpf.kprobe_poll()
