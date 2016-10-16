#!/usr/bin/python
# @lint-avoid-python-3-compatibility-imports
#
# setuidsnoop Trace uid changes issued by the setuid() syscall.
#             For Linux, uses BCC, eBPF. Embedded C.
#
# USAGE: setuidsnoop [-h] [-p PID]
#
# Copyright (c) 2016 Brendan Gregg, Sasha Goldshtein.
# Licensed under the Apache License, Version 2.0 (the "License")
#
# 20-Sep-2015   Brendan Gregg     Created killsnoop.
# 19-Feb-2016   Allan McAleavy    Migrated to BPF_PERF_OUTPUT.
# 16-Oct-2016   Sasha Goldshtein  Modified to setuidsnoop.

from __future__ import print_function
from bcc import BPF
import argparse
from time import strftime
import ctypes as ct

# arguments
examples = """examples:
    ./setuidsnoop         # trace all setuid() calls
    ./setuidsnoop -p 181  # only trace PID 181
"""
parser = argparse.ArgumentParser(
    description="Trace uid changes issued by the setuid() syscall",
    formatter_class=argparse.RawDescriptionHelpFormatter,
    epilog=examples)
parser.add_argument("-p", "--pid",
    help="trace this PID only")
args = parser.parse_args()
debug = 0

# define BPF program
bpf_text = """
#include <uapi/linux/ptrace.h>
#include <linux/sched.h>

struct val_t {
   u64 pid;
   u32 uid;
   char comm[TASK_COMM_LEN];
};

struct data_t {
   u64 pid;
   u32 uid;
   int ret;
   char comm[TASK_COMM_LEN];
};

BPF_HASH(infotmp, u32, struct val_t);
BPF_PERF_OUTPUT(events);

int kprobe__sys_setuid(struct pt_regs *ctx, u32 uid)
{
    u32 pid = bpf_get_current_pid_tgid();
    FILTER

    struct val_t val = {.pid = pid};
    if (bpf_get_current_comm(&val.comm, sizeof(val.comm)) == 0) {
        val.uid = uid;
        infotmp.update(&pid, &val);
    }

    return 0;
};

int kretprobe__sys_setuid(struct pt_regs *ctx)
{
    struct data_t data = {};
    struct val_t *valp;
    u32 pid = bpf_get_current_pid_tgid();

    valp = infotmp.lookup(&pid);
    if (valp == 0) {
        // missed entry
        return 0;
    }

    bpf_probe_read(&data.comm, sizeof(data.comm), valp->comm);
    data.pid = pid;
    data.uid = valp->uid;
    data.ret = PT_REGS_RC(ctx);

    events.perf_submit(ctx, &data, sizeof(data));
    infotmp.delete(&pid);

    return 0;
}
"""
if args.pid:
    bpf_text = bpf_text.replace('FILTER',
        'if (pid != %s) { return 0; }' % args.pid)
else:
    bpf_text = bpf_text.replace('FILTER', '')
if debug:
    print(bpf_text)

# initialize BPF
b = BPF(text=bpf_text)

TASK_COMM_LEN = 16    # linux/sched.h

class Data(ct.Structure):
    _fields_ = [
        ("pid", ct.c_ulonglong),
        ("uid", ct.c_uint),
        ("ret", ct.c_int),
        ("comm", ct.c_char * TASK_COMM_LEN)
    ]

# header
print("%-9s %-6s %-16s %-6s %s" % (
    "TIME", "PID", "COMM", "UID", "RESULT"))

# process event
def print_event(cpu, data, size):
    event = ct.cast(data, ct.POINTER(Data)).contents
    print("%-9s %-6d %-16s %-6d %d" % (strftime("%H:%M:%S"),
        event.pid, event.comm, event.uid, event.ret))

# loop with callback to print_event
b["events"].open_perf_buffer(print_event)
while 1:
    b.kprobe_poll()
