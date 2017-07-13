### Using BPF Tools: Broken File Opens

In this lab, you will experiment with diagnosing an application that fails to start correctly by using some of the BCC tools.

- - -

#### Task 1: Compile and Run the Application

First, run the following command to compile `server.c` into the server application you'll be diagnosing:

```
$ gcc -g -fno-omit-frame-pointer -O0 server.c -o server
```

Now, run `./server`. It should print a message saying that it's starting up, but it never completes initialization.

- - -

#### Task 2: Perform Basic Diagnostics

Because the application process seems to be stuck, let's try to see what it's doing. Run `top` -- you should see the app near the top of the output, consuming some CPU. Next, run the following command to see a report of the process' user and system CPU utilization every second: 

```
$ pidstat -u -p $(pidof server) 1
```

It looks like the process is spending a bit of time in kernel mode.

- - -

#### Task 3: Snoop Syscalls

If the process is running frequently in kernel mode, it must be making quite a bunch of syscalls. To characterize its workload, we can use the BCC `syscount` tool:

```
# syscount -p $(pidof server)
```

This collects all syscall events. Press Ctrl+C after a few seconds to stop collection. It looks like the application is calling `nanosleep()` and `open()` quite frequently.

- - -

#### Task 4: Snooping Opens

Fortunately, BCC has a tool for snooping all `open` calls performed by a certain process, including the path being opened and the result of the call. Run the following command to trace opens:

```
# opensnoop -p $(pidof server)
```

The problem becomes apparent -- the application is trying to open the `/etc/tracing-server-example.conf` file, and is getting a -1 result with an errno of 2, which stands for ENOENT (no such file or directory). Indeed, you can confirm that the file does not exist. You can even try creating the file and making sure the server now starts successfully.

- - -

#### Bonus: Use `argdist` for Argument Analysis

The `argdist` tool from BCC can be used for quick argument analysis when you're interested in the values an application passes to a certain function or syscall. In our case, let's get a histogram of sleep durations by tracing the `nanosleep()` syscall in the application process:

```
# argdist -p $(pidof server) -H 'p::SyS_nanosleep(struct timespec *time):u64:time->tv_nsec'
```

This prints a histogram (using -H) of the sleep durations, which seem to be concentrated in one specific bin: 512-1023 ns. Indeed, inspecting the application source code you can verify that it calls `usleep(1)`, which corresponds to 1000 nanoseconds.

Similarly, we could use `argdist` to get a frequency count (using -C) of which files the application is trying to open:

```
# argdist -p $(pidof server) -C 'p:c:open(char *filename):char*:filename'
```

And similarly, we could get a frequency count of return values from `open()`:

```
# argdist -p $(pidof server) -C 'r:c:open():int:$retval'
```

- - -
