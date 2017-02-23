### Using BPF Tools: `trace` and `argdist` One-Liners

In this lab, you will experiment with the multi-purpose `trace` and `argdist` tools, which place a lot of tracing power just a one-liner shell command away. This kind of ad-hoc analysis is very powerful, and provides a lot of observability into the system even if you don't have a dedicated, well-polished tool.

- - -

#### Task 1: Display All System Login Attempts with `trace`

Whenever a user logs into the system -- and in fact, even when you use `sudo` to run a command as a different user -- one of the `set*uid` syscall family is being invoked. By tracing these syscalls, we can obtain a complete trace of system login and `sudo` activity over time.

For example, let's trace `setuid` -- the corresponding syscall name is `sys_setuid`:

```
trace '::sys_setuid "uid = %d", arg1'
```

In another shell, run `sudo su` or establish a new SSH connection to the machine. In both cases you should see a trace message in the previous shell.

Now, try adding additional probe definitions to also trace `setreuid` and `setresuid`. The latter one has three parameters: the real user id, the effective user id, and the saved set-user-id.

- - -

#### Task 2: Identify Hot Files with `argdist`

Next, we're going to identify hot files (written or read frequently) by using `argdist` and probing specific kernel functions: `__vfs_write` and `__vfs_read`. This is a slightly brittle approach, but it's also the one used by a variety of BCC tools, including `fileslower` and `filetop`. (In fact, we are reproducing a tiny part of `filetop`'s capabilities: in production, you should probably use that tool instead.)

Run the following command to attach to the aforementioned `__vfs_write` and `__vfs_read` functions and collect the file name being accessed (with 5-second summaries):

```
argdist -T 5 -i 5 \
  -C 'p::__vfs_write(struct file *f):char*:f->f_path.dentry->d_name.name#writes' \
  -C 'p::__vfs_read(struct file  *f):char*:f->f_path.dentry->d_name.name#reads'
```

In another shell window, run the following command to generate artificial I/O:

```
dd if=/dev/zero of=/dev/null bs=1K count=1M
```

Observe the null and zero files showing up in `argdist`'s output as the hottest files. Also note that you could have filtered the output to a specific process by using the `-p` switch.

- - -

#### Task 3: Display PostgreSQL Queries with `trace`

> Before starting this task, you will need the PostgreSQL database running. If you followed the installation instructions, and compiled PostgreSQL from source, you should be able to run it from /usr/local/pgsql with a command similar to `sudo -u postgres /usr/local/pgsql/bin/postgres -D /usr/local/pgsql/data &`.

In this task, we are going to trace PostgreSQL queries by using the USDT probes embedded into the database engine. Run the `psql` client (change the path if necessary according to your system):

```
sudo -u postgres /usr/local/pgsql/bin/psql
```

In another shell, run the following command to discover all the probes embedded into the PostgreSQL process:

```
tplist -p $(pgrep -n postgres) | grep postgres
```

As you see, there are a number of probes for query execution, such as `query__start`, `query__execute__start`, `query__done`, `query__execute__done`, and some others. We are going to use the first one of these, which takes the executed query as its first parameter. The problem is finding the process id to attach to -- you can see all the postgres processes by running `ps -ef | grep postgres`, and pick the backend process for your client connection (experimenting until you find the right one):

```
trace -p $POSTGRES_PID 'u:/usr/local/pgsql/bin/postgres:query__start "%s", arg1'
```

Now, go back to the psql shell and run some SQL statements. For example:

```
create table info (id integer primary key, name varchar(200));
insert into info values (1, "Chair");
select * from info;
```

If you picked the right process id, you should see trace statements after you run each query. If not, try again with another id.

- - -

#### Task 4: Display a PostgreSQL Query Latency Histogram with `argdist`

Once you have the ability to trace queries, it becomes interesting to trace their latency as well. This is not very easy with the USDT probes, because there are two distinct probes we need to trace: `query__start` and `query__done`. Then, we'd need to subtract the end time from the start time to determine the latency -- all of which sounds like too much work for a one-liner. Indeed, in the [`dbslower`](bpf-dbslower.md) lab you will write a custom tool for tracing PostgreSQL/MySQL queries.

For now, if we want to keep using `argdist`, we need to find a single function to probe. Then, `argdist` will attach to its entry and exit points, and aggregate latency automatically for us.

Although it doesn't cover the entire time of query execution, we are going to probe the `PortalRun` function (it doesn't include query parsing and planning costs). To trace the latency of that function as a histogram, run the following command:

```
argdist -c -i 5 -H 'r:/usr/local/pgsql/bin/postgres:PortalRun():u64:$latency/1000000#latency (ms)'
```

Copy the [pg-slow.sql](pg-slow.sql) file to /tmp and run `sudo chown postgres /tmp/pg-slow.sql` to let the postgres user access it. Then, hop over to the psql shell (run psql again if you don't have it open) and run the script, while at the same time monitoring the latency output from `argdist`:

```
\i /tmp/pg-slow.sql
```

- - -
