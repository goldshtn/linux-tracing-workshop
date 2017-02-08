#!/usr/bin/python
#
# dbstat        Display a histogram of MySQL and PostgreSQL query latencies.
#
# USAGE: dbstat {mysql,postgres} [-v]
# queries (verbose).
#
# This tool uses USDT probes, which means it needs MySQL and PostgreSQL built
# with USDT (DTrace) support.
#
# Licensed under the Apache License, Version 2.0
#

from bcc import BPF, USDT
import argparse
import subprocess
from time import sleep

examples = """
    dbstat postgres   # display a histogram of PostgreSQL query latencies
    dbstat mysql -v   # display MySQL query latencies and print the BPF program
    dbstat mysql -m   # display query latencies in milliseconds (default: us)
    dbstat mysql 5    # trace only queries slower than 5ms
"""
parser = argparse.ArgumentParser(
    description="",
    formatter_class=argparse.RawDescriptionHelpFormatter,
    epilog=examples)
parser.add_argument("-v", "--verbose", action="store_true",
    help="print the BPF program")
parser.add_argument("-m", "--milliseconds", action="store_true",
    help="display query latencies in milliseconds (default: microseconds)")
parser.add_argument("db", choices=["mysql", "postgres"],
    help="the database engine to use")
parser.add_argument("threshold", type=int, default=0, nargs='?',
    help="trace only queries slower than this threshold (default: 0)")
args = parser.parse_args()

if args.db == "mysql":
    dbpid = int(subprocess.check_output("pidof mysqld".split()))
elif args.db == "postgres":
    dbpid = int(subprocess.check_output("pgrep -n postgres".split()))

program = """
#include <linux/ptrace.h>

BPF_HASH(temp, u64, u64);
BPF_HISTOGRAM(latency);

int probe_start(struct pt_regs *ctx) {
    u64 timestamp = bpf_ktime_get_ns();
    u64 pid = bpf_get_current_pid_tgid();
    temp.update(&pid, &timestamp);
    return 0;
}

int probe_end(struct pt_regs *ctx) {
    u64 *timestampp;
    u64 pid = bpf_get_current_pid_tgid();
    timestampp = temp.lookup(&pid);
    if (!timestampp)
        return 0;

    u64 delta = bpf_ktime_get_ns() - *timestampp;
    if (delta/1000000 < %d)
        return 0;

    delta /= %d;
    latency.increment(bpf_log2l(delta));
    temp.delete(&pid);
    return 0;
}
""" % (args.threshold, 1000000 if args.milliseconds else 1000)

usdt = USDT(pid=int(dbpid))
usdt.enable_probe("query__start", "probe_start")
usdt.enable_probe("query__done", "probe_end")

bpf = BPF(text=program, usdt_contexts=[usdt])
if args.verbose:
    print(usdt.get_text())
    print(program)

print("Tracing database queries slower than %dms for PID %d... Ctrl+C to quit."
        % (args.threshold, dbpid))

try:
    sleep(999999999999)
except KeyboardInterrupt:
    pass

bpf["latency"].print_log2_hist("query latency (%s)" %
                               ("ms" if args.milliseconds else "us"))
