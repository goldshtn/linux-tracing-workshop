#!/usr/bin/env python

from bcc import BPF, USDT
import argparse

parser = argparse.ArgumentParser(
    description="Trace JVM monitor contention events and dump a summary",
    formatter_class=argparse.RawDescriptionHelpFormatter)
parser.add_argument("pid", type=int, help="the Java process id")
args = parser.parse_args()

usdt = USDT(pid=args.pid)
usdt.enable_probe("monitor__contended__enter", "trace_enter")
usdt.enable_probe("monitor__contended__entered", "trace_entered")

bpf = BPF(text="""
int trace_enter(struct pt_regs *ctx) {
    // TODO
    return 0;
}

int trace_entered(struct pt_regs *ctx) {
    // TODO
    return 0;
}
""", usdt_contexts=[usdt])

# TODO
