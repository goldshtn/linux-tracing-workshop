### Using `perf` Tools: Broken File Opens

In this lab, you will experiment with diagnosing an application that fails to start correctly by using some of the tools in the perf-tools toolkit.

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

> For the rest of this lab, you will need the perf-tools toolkit on your path, or you will need to prefix the commands with the full path to where you placed the [perf-tools](https://github.com/brendangregg/perf-tools) repository.

If the process is running frequently in kernel mode, it must be making quite a bunch of syscalls. To characterize which syscalls it's making, use the `syscount` tool:

```
# syscount -c -p $(pidof server)
```

> NOTE: There's a BCC tool called syscount, and a perf-tools tool called syscount. If you have both installed and in the path (like in the lab environment), you will need to qualify the specific one -- in this case, the one in the perf-tools folder.

This collects all syscall events. Press Ctrl+C after a few seconds to stop collection. As you can see, the application is making a lot of `nanosleep()` and `open()` calls, but doesn't seem to be making progress. This looks like a typical retry pattern -- try to open a file, fail, and then try again.

- - -

#### Task 4: Snooping Opens

Fortunately, there a tool for snooping all `open` calls performed by a certain process, including the path being opened and the result of the call. Run the following command to trace opens:

```
# opensnoop -p $(pidof server)
```

The problem becomes apparent -- the application is trying to open the `/etc/tracing-server-example.conf` file, and is getting a -1 result. Indeed, you can confirm that the file does not exist. You can even try creating the file and making sure the server now starts successfully.

- - -
