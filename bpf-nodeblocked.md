### Using BPF Tools: Node Blocked Time Analysis

In this lab, you will experiment with identifying blocked time in a Node application. Because Node apps have only one thread for running JavaScript code, any blocking can be disastrous, and figuring out why the thread was blocked can be hard. However, with a tool designed for monitoring context switches and aggregating them in a lightweight way, blocking bottlenecks can be easily found and fixed.

- - -

#### Task 1: Run the Slow App

Navigate to the `nodey` directory. If you haven't yet, you should make sure all required packages are installed by running `npm install`. Then, run the following command to start our simple Node application (the `perf` subcommand makes the script invoke the Node runtime with `--perf_basic_prof`, which generates a map file needed for translating addresses to method names):

```
$ ./run.sh perf
```

Now, run `top` in one window, and the following command in another window, to monitor the CPU utilization of the app while we hit the `/stats` endpoint:

```
$ ab -n 10 http://localhost:3000/stats
```

There is no visible CPU activity in the Node process, but the benchmark results are not great: the response time is not nearly instantaneous. To figure out what's happening, we need to look beyond just CPU time, and inspect off-CPU time as well, also known as blocked time.

- - -

#### Task 2: Collect Off-CPU Stacks with `offcputime`

The `offcputime` tool from [BCC](https://github.com/iovisor/bcc) can collect off-CPU stacks for your application's threads, and aggregate them in a way suitable for flame graph generation or direct browsing. Run the following command in a root shell:

```
# offcputime -p $(pgrep -n node) -f > folded.stacks
```

...and in the original shell, run the benchmark command again:

```
$ ab -n 10 http://localhost:3000/stats
```

Go back to the root shell and hit Ctrl+C so that the tool finalizes the recording. The folded.stacks file is now ready; you can take a look at it directly -- it contains stacks from the Node application when the thread was blocked, i.e. transitioning _off_ the CPU (as opposed to traditional CPU profiling, which looks only at _on_-CPU time).

It is probably easier to generate a flame graph from the recorded stacks to see what the event thread is doing:

```
$ cat folded.stacks | FlameGraph/flamegraph.pl > offcputime.svg
```

It should become evident that the application is spending lots of time writing to files, and doing so _synchronously_ from the event loop thread. This blocks further request processing, effectively serializing the handling of these requests. One option would be to rewrite this section of the code to use asynchronous file operations, but first, we need to understand which files are being accessed, and why. Perhaps the file writes are really small, and we simply have a defective
disk, which is making these writes execute so slowly?

> NOTE: Detecting synchronous operations on the event thread is also possible using the `--trace-sync-io` command-line switch. Try launching Node directly with this switch, and seeing the exact call stacks responsible. This doesn't help, though, when background threads issue the slow I/O operations, or when you're investigating a production issue.

- - -

#### Task 3: Identify Files Being Accessed with `fileslower`

Now that we know the application is spending a lot of time synchronously writing to files, we want to identify which files are involved, the write sizes, and the individual operation latencies. Even though this application is issuing writes, the same technique can be used for reads; e.g., to identify which static files being served by the application can be cached at the application level or by an upstream proxy.

Run the following command, which uses the `fileslower` tool from BCC, to get a capture of all the file accesses performed by the Node process taking longer than 1ms:

```
# fileslower -p $(pgrep -n node) 1
```

While this is executing in the root shell, run the benchmark one more time in the original shell:

```
$ ab -n 10 http://localhost:3000/stats
```

In the root shell, `fileslower` now shows the exact files being written, the write sizes, and the latency of each individual operation. It also shows clearly that there are _large_ file writes being performed synchronously, so the disk is probably not to blame for the latency.

- - -
