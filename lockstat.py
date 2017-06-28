#!/usr/bin/env python

import sys
import itertools
from time import sleep
from bcc import BPF

text = """
#include <linux/ptrace.h>

struct thread_mutex_key_t {
    u32 tid;
    u64 mtx;
    int lock_stack_id;
};

struct thread_mutex_val_t {
    u64 wait_time_ns;
    u64 lock_time_ns;
    u64 enter_count;
};

struct mutex_timestamp_t {
    u64 mtx;
    u64 timestamp;
};

struct mutex_lock_time_key_t {
    u32 tid;
    u64 mtx;
};

struct mutex_lock_time_val_t {
    u64 timestamp;
    int stack_id;
};

// Mutex to the stack id which initialized that mutex
BPF_HASH(init_stacks, u64, int);

// Main info database about mutex and thread pairs
BPF_HASH(locks, struct thread_mutex_key_t, struct thread_mutex_val_t);

// Pid to the mutex address and timestamp of when the wait started
BPF_HASH(lock_start, u32, struct mutex_timestamp_t);

// Pid and mutex address to the timestamp of when the wait ended (mutex acquired) and the stack id
BPF_HASH(lock_end, struct mutex_lock_time_key_t, struct mutex_lock_time_val_t);

// Histogram of wait times
BPF_HISTOGRAM(mutex_wait_hist, u64);

// Histogram of hold times
BPF_HISTOGRAM(mutex_lock_hist, u64);

BPF_STACK_TRACE(stacks, 4096);

int probe_mutex_lock(struct pt_regs *ctx)
{
    u64 now = bpf_ktime_get_ns();
    u32 pid = bpf_get_current_pid_tgid();
    struct mutex_timestamp_t val = {};
    val.mtx = PT_REGS_PARM1(ctx);
    val.timestamp = now;
    lock_start.update(&pid, &val);
    return 0;
}

int probe_mutex_lock_return(struct pt_regs *ctx)
{
    u64 now = bpf_ktime_get_ns();

    u32 pid = bpf_get_current_pid_tgid();
    struct mutex_timestamp_t *entry = lock_start.lookup(&pid);
    if (entry == 0)
        return 0;   // Missed the entry

    u64 wait_time = now - entry->timestamp;
    int stack_id = stacks.get_stackid(ctx, BPF_F_REUSE_STACKID|BPF_F_USER_STACK);

    // If pthread_mutex_lock() returned 0, we have the lock
    if (PT_REGS_RC(ctx) == 0) {
        // Record the lock acquisition timestamp so that we can read it when unlocking
        struct mutex_lock_time_key_t key = {};
        key.mtx = entry->mtx;
        key.tid = pid;
        struct mutex_lock_time_val_t val = {};
        val.timestamp = now;
        val.stack_id = stack_id;
        lock_end.update(&key, &val);
    }

    struct thread_mutex_key_t tm_key = {};
    // TODO Update tm_key fields with the mutex, tid, and stack id
    // TODO Call locks.lookup_or_init(...) and update the wait time and the enter count
    //      of the entry in the locks data structure

    u64 mtx_slot = bpf_log2l(wait_time / 1000);
    mutex_wait_hist.increment(mtx_slot);

    lock_start.delete(&pid);

    return 0;
}

int probe_mutex_unlock(struct pt_regs *ctx)
{
    u64 now = bpf_ktime_get_ns();
    u64 mtx = PT_REGS_PARM1(ctx);
    u32 pid = bpf_get_current_pid_tgid();
    struct mutex_lock_time_key_t lock_key = {};
    lock_key.mtx = mtx;
    lock_key.tid = pid;
    struct mutex_lock_time_val_t *lock_val = lock_end.lookup(&lock_key);
    if (lock_val == 0)
        return 0;   // Missed the lock of this mutex

    u64 hold_time = now - lock_val->timestamp;

    struct thread_mutex_key_t tm_key = {};
    tm_key.mtx = mtx;
    tm_key.tid = pid;
    tm_key.lock_stack_id = lock_val->stack_id;
    struct thread_mutex_val_t *existing_tm_val = locks.lookup(&tm_key);
    if (existing_tm_val == 0)
        return 0;   // Couldn't find this record
    existing_tm_val->lock_time_ns += hold_time;

    // TODO Update the mutex_lock_hist histogram with the time we held the lock

    lock_end.delete(&lock_key);

    return 0;
}

int probe_mutex_init(struct pt_regs *ctx)
{
    int stack_id = stacks.get_stackid(ctx, BPF_F_REUSE_STACKID|BPF_F_USER_STACK);
    u64 mutex_addr = PT_REGS_PARM1(ctx);
    init_stacks.update(&mutex_addr, &stack_id);
    return 0;
}
"""

def attach(bpf, pid):
    bpf.attach_uprobe(name="pthread", sym="pthread_mutex_init", fn_name="probe_mutex_init", pid=pid)
    # TODO Similarly to the previous probe, attach the following probes:
    #   uprobe    in pthread_mutex_lock   handled by probe_mutex_lock
    #   uretprobe in pthread_mutex_lock   handled by probe_mutex_lock_return
    #   uprobe    in pthread_mutex_unlock handled by probe_mutex_unlock

def print_frame(bpf, pid, addr):
    print("\t\t%16s (%x)" % (bpf.sym(addr, pid, show_module=True, show_offset=True), addr))

def print_stack(bpf, pid, stacks, stack_id):
    for addr in stacks.walk(stack_id):
        print_frame(bpf, pid, addr)

def run(pid):
    bpf = BPF(text=text)
    attach(bpf, pid)
    init_stacks = bpf["init_stacks"]
    stacks = bpf["stacks"]
    locks = bpf["locks"]
    mutex_lock_hist = bpf["mutex_lock_hist"]
    mutex_wait_hist = bpf["mutex_wait_hist"]
    while True:
        sleep(5)
        mutex_ids = {}
        next_mutex_id = 1
        for k, v in init_stacks.items():
            mutex_id = "#%d" % next_mutex_id
            next_mutex_id += 1
            mutex_ids[k.value] = mutex_id
            print("init stack for mutex %x (%s)" % (k.value, mutex_id))
            print_stack(bpf, pid, stacks, v.value)
            print("")
        grouper = lambda (k, v): k.tid
        sorted_by_thread = sorted(locks.items(), key=grouper)
        locks_by_thread = itertools.groupby(sorted_by_thread, grouper)
        for tid, items in locks_by_thread:
            print("thread %d" % tid)
            for k, v in sorted(items, key=lambda (k, v): -v.wait_time_ns):
                mutex_descr = mutex_ids[k.mtx] if k.mtx in mutex_ids else bpf.sy(k.mtx, pid)
                # TODO Print a nicely formatted line with the mutex description, wait time,
                #      hold time, enter count, and stack (use print_stack)
                print("")
        mutex_wait_hist.print_log2_hist(val_type="wait time (us)")
        mutex_lock_hist.print_log2_hist(val_type="hold time (us)")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("USAGE: %s pid" % sys.argv[0])
    else:
        run(int(sys.argv[1]))
