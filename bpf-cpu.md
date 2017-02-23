### Using BPF Tools: CPU and Off-CPU Investigation

In this lab, you will work with several tools from BCC to investigate an application that is supposed to be CPU-bound and active, but in fact spends up a lot of time off-CPU for some reason.

- - -

#### Task 1: Basic Workload Characterization

Run the following command to compile the target application:

```
$ gcc -g -fno-omit-frame-pointer -fno-inline -lpthread blocky.c -o blocky     # on Fedora
$ gcc -g -fno-omit-frame-pointer -fno-inline -pthread  blocky.c -o blocky     # on Ubuntu
```

Now, run `./blocky`. It prints out the number of requests processed, going at a nice pace. But is it really using all processors effectively? Run `top` (or `htop`) to find out -- it looks like the application is barely making a dent in CPU utilization. Most of the time, the system is idle.

- - -

#### Task 2: Profiling The CPU-Bound Part

> As with all BPF-based tools, you will need a root shell to run the following commands.

To understand where the application is spending CPU time (which, as we saw, is not most of the wall-clock time), run the following command:

```
profile -F 997 -p $(pidof blocky)
```

> Note: The `profile` tool requires BCC support for `perf_events`, which was introduced in Linux 4.9. If you are running an older kernel, this tool will not work. You can use `perf record` instead, as discussed in [a previous lab](perf.md).

After a few seconds, hit Ctrl+C to stop the `profile` tool. The call stacks you get should point to the `request_processor` and `do_work` functions, which are supposed to burn a lot of CPU. But, as we saw above, there's something blocking the application from making progress.

> Bonus: You can also use `profile` to get flame graphs by using the `-f` switch, which will output folded stacks in the exact format that the flamegraph.pl script expects. If you have time, run `profile -F 997 -f -p $(pidof blocky) > folded-stacks`, hit Ctrl+C after a few seconds, and then run `flamegraph.pl folded-stacks > profile.svg` to generate the flame graph. It's important to mention that this method of generating flame graphs is a lot more efficient than using [`perf`](perf.md), because the stack aggregation (folding) is performed in-kernel rather than in user-space.

- - -

#### Task 3: Identifying On- and Off-CPU Time

To determine how much time the application is spending on-CPU and off-CPU, the `cpudist` tool can be very useful. It produces a histogram of time spent in each of the two CPU states, and helps understand the context-switching characteristics of your application. Let's run it:

```
cpudist -p $(pidof blocky)
```

After a few seconds, hit Ctrl+C to get a report. You will probably see a bimodal distribution, with a mode around the quantum length on your system (which is typically in the 4-8ms bin), and another, bigger mode around very short intervals -- tens or hundreds of microseconds each. It is these smaller intervals that we should be worried about. These bursts of activity indicate that the application threads want to do some work, and are successful at doing it for a very short time, but then they get switched out.

Now, let's try to identify how long the application threads spend off-CPU, using the same tool:

```
cpudist -O -p $(pidof blocky)
```

Again, hit Ctrl+C after a few seconds to get the histogram. You might get a bimodal distribution again, but the biggest mode this time is in the much longer bins -- 8-16ms and 16-32ms. Again, these are time intervals when some application thread was off the CPU waiting for something. So we have a great number of multi-millisecond waits. Where are these waits coming from?

- - -

#### Task 4: Getting Off-CPU Stacks

The `offcputime` tool from BCC can be used to answer this final question. It collects stack traces whenever an application thread is blocked (switched off the CPU), and identifies the duration of time that blockage lasted. The output can be interpreted as a flame graph, much like on-CPU data. Let's run it in folded mode to generate data suitable for producing a flame graph:

```
offcputime -f -p $(pidof blocky) > folded-stacks
```

After a few seconds, hit Ctrl+C. At this point, you can also stop the blocky process. To generate a flame graph, run:

```
flamegraph.pl folded-stacks > offcpu.svg
```

Inspecting the flame graph should show two sources of contention: one in the `backend_handler` function, which calls `nanosleep`, and another in the `request_processor` function, which calls `__lll_lock_wait`, which calls the `futex` syscall. These are specific, identifiable places in the source code that can now be inspected to understand why they're causing blockages. If you review [blocky.c](blocky.c), you'll find that the `backend_handler` function is the main culprit: it acquires a lock which the `request_processor` threads also require to make progress, and then occasionally sleeps while holding the lock!

- - -
