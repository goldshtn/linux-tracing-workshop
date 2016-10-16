
### Authoring BPF Tools: `dbslower` 

In this lab, you will develop a BCC tool based on USDT probes to monitor query latency and execution in a couple of popular database engines: MySQL and PostgreSQL.

- - -

#### Task 1: Inspect MySQL and PostgreSQL USDT Probes

Both MySQL and PostgreSQL are equipped with USDT probes, which enable you to inspect query execution and latency, among dozens of other metrics. Begin by checking out the documentation for [MySQL USDT probes](https://dev.mysql.com/doc/refman/5.7/en/dba-dtrace-mysqld-ref.html) and [PostgreSQL USDT probes](https://www.postgresql.org/docs/current/static/dynamic-trace.html).

> NOTE: While MySQL is compiled with USDT probes by default (for most distributions), PostgreSQL needs to be built from source with the `--enable-dtrace` configure flag. If your MySQL installation does not have USDT probes either, build MariaDB from [source](https://github.com/MariaDB/server) with the `ENABLE_DTRACE` cmake option.

Start the two database engines, unless you already have them running on your system (these instructions assume that they have been installed to `/usr/local`):

```
$ cd /usr/local/pgsql/bin
$ ./initdb -D /tmp/pgdata
$ ./pg_ctl -D /tmp/pgdata start

$ cd /usr/local/mysql
$ sudo chown -R $(whoami) .
$ sudo chgrp -R $(whoami) .
$ scripts/mysql_install_db --user=$(whoami)
$ sudo chown -R root .
$ sudo chown -R $(whoami) data
$ bin/mysqld_safe --user=$(whoami)
```

Now, use the `tplist` tool to take a look at some of the probes embedded in the MySQL and PostgreSQL processes:

```
# tplist -p $(pgrep -n postgres)
# tplist -p $(pidof mysqld)
```

Specifically, look for `query__start` and `query__done` probes. These will be useful for tracing slow queries. In both database engines, the first argument of the `query__start` probe is a pointer to the query string, which means we'll be able to print the query as well as its duration.

- - -

#### Task 2: Tracing Queries

Create a new script file, `dbslower.py`. You can copy the following skeleton code that sets up some imports and command line arguments, or write the tool from scratch with your own arguments.

```python
#!/usr/bin/python
#
# dbslower      Trace MySQL and PostgreSQL queries slower than a threshold.
#
# USAGE: dbslower {mysql,postgres} [threshold_ms] [-v]
#
# By default, a threshold of 1ms is used. Set the threshold to 0 to trace all
# queries (verbose).
#
# This tool uses USDT probes, which means it needs MySQL and PostgreSQL built
# with USDT (DTrace) support.
#
# Licensed under the Apache License, Version 2.0
#

from bcc import BPF, USDT
import argparse
import ctypes as ct

examples = """
    dbslower postgres   # trace PostgreSQL queries slower than 1ms
    dbslower mysql 30   # trace MySQL queries slower than 30ms
    dbslower mysql -v   # trace MySQL queries and print the BPF program
"""
parser = argparse.ArgumentParser(
    description="",
    formatter_class=argparse.RawDescriptionHelpFormatter,
    epilog=examples)
parser.add_argument("-v", "--verbose", action="store_true",
    help="print the BPF program")
parser.add_argument("db", choices=["mysql", "postgres"],
    help="the database engine to use")
parser.add_argument("threshold", type=int, nargs="?", default=1,
    help="trace queries slower than this threshold (ms)")
args = parser.parse_args()

# TODO 1. find the pid for MySQL/PostgreSQL and store it in 'dbpid'

program = """
// TODO 2. declare perf output structure

// TODO 3. build handlers for query__start and query__end
"""

usdt = USDT(pid=int(dbpid))
# TODO 4. enable the query__start and query__end probes

bpf = BPF(text=program, usdt_contexts=[usdt])
if args.verbose:
    print(usdt.get_text())
    print(program)

# TODO 5. declare perf output structure

# TODO 6. call open_perf_buffer to set up event printing
while True:
    bpf.kprobe_poll()
```

Fill in the missing parts. Make sure to use the [tutorial](https://github.com/iovisor/bcc/blob/master/docs/tutorial_bcc_python_developer.md) and [reference guide](https://github.com/iovisor/bcc/blob/master/docs/reference_guide.md). Here are some tips:

1. Find the MySQL or PostgreSQL database process, depending on the `args.db` value. You can simply invoke `pidof mysqld` and `pgrep -n postgres` using the [subprocess](https://docs.python.org/2.7/library/subprocess.html) module.
1. Declare an output structure for use with the `BPF_PERF_OUTPUT` macro. This structure will be used to pass query information to the Python script. You will probably want to include the PID, timestamp, duration, and query text here. See the [reference guide](https://github.com/iovisor/bcc/blob/master/docs/tutorial_bcc_python_developer.md#lesson-7-hello_perf_outputpy) for more help.
1. Write two functions that will handle the `query__start` and `query__end` probes. These functions should return an int, and take a single parameter of type `struct pt_regs *`. Inside the functions, you can use the `bpf_usdt_readarg` macro to read the probe's arguments. Specifically, in `query__start`'s handler you will need the first argument to get the query text. At this time, you can store the query text in the output structure and submit it to the Python program using `perf_submit`. This means you don't really need to put anything in your `query__done` handler at this time. In the next step, you will also want the duration of the query, which means you'd have to wait for the `query__done` handler to get the duration. See the [reference guide](https://github.com/iovisor/bcc/blob/master/docs/tutorial_bcc_python_developer.md#lesson-15-nodejs_http_serverpy) for more help.
1. Use `enable_probe` to enable the `query__start` and `query__end` probes, and attach them to the handlers you previously declared in the C program. See the [reference guide](https://github.com/iovisor/bcc/blob/master/docs/tutorial_bcc_python_developer.md#lesson-15-nodejs_http_serverpy) for more help.
1. Declare the Python structure that mirrors the output structure from the C code. This is a class that inherits from `ct.Structure`. See the [reference guide](https://github.com/iovisor/bcc/blob/master/docs/tutorial_bcc_python_developer.md#lesson-7-hello_perf_outputpy) for more help.
1. Write a function that prints individual events. It should take the CPU number, the event data structure, and its size. You probably only care about the actual structure, which you should cast to the Python structure you declared in the previous step and then access its fields for printing. This function should be passed to `open_perf_buffer`. See the [reference guide](https://github.com/iovisor/bcc/blob/master/docs/tutorial_bcc_python_developer.md#lesson-7-hello_perf_outputpy) for more help.

Test your tool by running it alongside with some queries. The simplest way to run queries is using the `mysql` application for MySQL and the `psql` tool for PostgreSQL. For example:

```
$ cd /usr/local/pgsql/bin
$ ./psql -d postgres
psql (10devel)
Type "help" for help.

postgres=# select * from foo;
^D

$ cd /usr/local/mysql/bin
$ ./mysql
...
MariaDB [(none)]> use test;
Database changed
MariaDB [test]> select * from foo;
^D
```

- - -

#### Task 3: Tracing Slow Queries

Next, you'll add support for determining the query duration, and only tracing queries slower than the user-specified threshold. Up until now, you were pushing the query to user space in the `query__start` handler. Now, you'll need to defer that step to the `query__done` handler, which is when you know the query's duration and whether the duration was greater than the threshold. However, in the `query__done` handler you no longer have access to the query text. You have to store the query text in the `query__start` handler and then retrieve it in the `query__done` handler.

This is a fairly common problem with a standard solution: use a map (hash table, dictionary) to share data between the handlers. [Lesson 6](https://github.com/iovisor/bcc/blob/master/docs/tutorial_bcc_python_developer.md#lesson-6-disksnooppy) in the Reference Guide covers this technique. In this case, you should probably use the pid as the map key to store the query text in your `query__start` handler, and retrieve the text in your `query__done` handler. The [`bpf_get_current_pid_tgid()`](https://github.com/iovisor/bcc/blob/master/docs/reference_guide.md#3-bpf_get_current_pid_tgid) function is your friend.

Here's a simple example of what this may look like:

```c
BPF_HASH(temp, u32, u64);   // temp is the name, u32 is the key type (pid), u64 is the value type

int probe_start(struct pt_regs *ctx) {
    u32 pid = bpf_get_current_pid_tgid();
    u64 val = ...;         // this is some value you need to store
    temp.update(&pid, &val);
    return 0;
}

int probe_end(struct pt_regs *ctx) {
    u32 pid = bpf_get_current_pid_tgid();
    u64 *valp = temp.lookup(&pid);
    if (!valp) {
        return 0;         // no matching value found in map
    }
    u64 val = *valp;      // use the value
    temp.delete(&pid);
    return 0;
}
```

> NOTE: There are two ways to address the threshold requirement. One option is filter queries faster than the threshold in the C part of the program (loaded into the kernel), and the other is to pass all queries and their duration to the Python script in user space and do the filtering there. What do you think is the better option?

Test the final result by running `./dbslower.py {mysql,postgres} 0` to see all queries. Then, use a larger threshold and attempt to run slower queries, e.g. by inserting a large number of values into some temporary table.

- - -

