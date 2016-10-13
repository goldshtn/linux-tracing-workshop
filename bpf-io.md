### Using BPF Tools: Database and Disk Stats and Stacks

In this lab, you will experiment with some of the task-focused tools from BCC to monitor disk performance, and, as a bonus, trace the queries performed by a database application.

- - -

#### Task 1: Monitor MySQL Insert Operations

You will need to generate a bunch of data so we can monitor the database while inserting items and while querying them. To do so, use the provided [data_access.py](data_access.py) script. It takes a single command-line argument, which can be either `insert`, `insert_once`, or `select`. But first, we need to make sure MySQL is running:

```
# systemctl start mariadb   # on Fedora
# systemctl start mysql     # on Ubuntu
```

Now run the insert script using the following command (it will run in an infinite loop):

```
$ ./data_access.py insert
```

In another root shell window, run the following command to get a quick reading at the block I/O operations performed by MySQL while you are inserting rows:

```
# biotop
```

You should see a display refreshing every second with the top disk I/O consumers. What's the I/O rate of the mysqld processes while the insert operations are running? It doesn't look like the disk is getting very busy. Run the following command to see the actual details of individual I/Os:

```
# biosnoop
```

How about the latency distribution? Are there any particularly slow I/Os?

```
# biolatency 5
```

OK, so we are seeing some block I/O being submitted. Let's take a look at the call stacks submitting these I/Os in the kernel, at 10 second intervals:

```
# stackcount -i 10 submit_bio
```

It seems that there are quite a few I/O operations. Let's take a look at some of the slower ones (slower than 1ms):

```
# fileslower 1
```

OK, so which files are being touched by mysqld while you're inserting rows? Run the following command to find out:

```
# filetop
```

Finally, what kind of interrupt load are these disk operations putting on the system? Run the following command to get a per-second summary of time spent servicing various interrupts:

```
# hardirqs 1
```

- - -

#### Task 2: Monitor MySQL Queries

You are going to experiment with queries now, so start by running the following command to get 10000 records in the database:

```
$ ./data_access.py insert_once
```

Next, run the following command to perform an infinite stream of queries on the data you just put in the database:

```
$ ./data_access.py select
```

In another root shell window, run a couple of the block I/O statistics commands from the previous task. Do you see any block I/Os being performed? Why do you think that is?

One possible cause is that the data we need is being served from the file system cache. Stop the select script and run the following command:

```
# cachestat 1
```

Now run the select script again. You'll notice that there are some file accesses (all of which should go through cache successfully), but then they stop and the select script keeps running with no file accesses at all, even though cache. So it looks like MySQL is caching the results *internally*, and not using the system's file system cache. To verify this, while the select script is still running, clear the file system cache:

```
# echo 1 > /proc/sys/vm/drop_caches
```

- - -

#### Bonus: Monitor MySQL Queries with `trace` and `argdist`

In addition to the generic scripts from the previous sections, we can instrument MySQL more accurately because it uses USDT for static tracing. (Probing USDT is covered in [the next lab](bpf-usdt.md).)

While the insert script is running, execute the following command to trace the insert statements as they occur:

```
# trace -p $(pidof mysqld) 'u:/usr/libexec/mysqld:query__start "%s", arg1'
```

You can see that there is a commit statement after every insert statement. Now let's get some statistics over the select script. Run it first, and then the following command to determine how many rows we're getting out of each select:

```
# argdist -p $(pidof mysqld) -C 'u:/usr/libexec/mysqld:select__done():u64:arg2#rows returned'
```

You can experiment with additional USDT probe points embedded in MySQL now, or wait for [the USDT lab](bpf-usdt.md). You can find a list of them [here](https://github.com/MariaDB/server/blob/10.1/include/probes_mysql.d.base).

- - -
