### Using BPF Tools: Node Garbage Collections

In this lab, you will experiment with a Node application that allocates a lot of memory, causing significant garbage collection delays. You will identify the number of GCs, their latencies, and the code locations making large memory allocations that cause the GC to kick in.

- - -

#### Task 1: Monitor Garbage Collections with `nodegc`

Navigate to the `nodey` directory. If you haven't yet, you should make sure all required packages are installed by running `npm install`. Then, run the following command to start the Node application:

```
$ ./run.sh
```

In a root shell, run the following command, which uses the `nodegc` tool from [BCC](https://github.com/iovisor/bcc) for monitoring Node garbage collections (it relies on a version of Node built with USDT probes):

```
# nodegc $(pgrep -n node)
```

Back in the original shell, run the following command to generate some load, whcih causes many garbage collections:

```
$ ab -n 10 http://localhost:3000/users
```

In the `nodegc` shell, note that each individual GC event is printed, along with a timestamp and a duration. Even though there's no single super-long GC, the multi-millisecond pauses add up and can cause significant problems if your application is placed under heavy load.

> NOTE: The Node runtime has a built-in switch for tracing garbage collection events, which you can turn on when your launch the application, using the `--trace_gc` and/or `--trace_gc_verbose` command-line arguments. You need to do this ahead-of-time, though, and the information is only available through the standard output. You can experiment with this option, too, to understand the trade-off.

- - -

#### Task 2: Get a Histogram of Garbage Collection Durations

The `nodegc` tool is nice, but it prints a lot of messages if your application is causing many garbage collections. What we might be after in many cases is rather a simple histogram showing garbage collection frequency and latency. There's a generic tool in BCC, called `funclatency`, which can attach to an arbitrary function and produce a histogram of its running times. We only need to discover the right function, and we're set.

There are several interesting functions related to garbage collection in [heap.cc](https://github.com/nodejs/node/blob/2db2857c72c219e5ba1642a345e52cfdd8c44a66/deps/v8/src/heap/heap.cc) on the Node.js source repo. Take a look at some of these: `PerformGarbageCollection`, `CollectGarbage`, `MarkCompact`, `Scavenge`, and others sound promising. The nice thing about tracing arbitrary functions is that we don't need any special probes embedded in the application, or any tracing
flags enabled ahead of time.

Run the following command from a root shell to get a latency histogram of any function in the Node binary that has `PerformGarbageCollection` in its name, using the `funclatency` tool:

```
# funclatency -m "$(which node):*PerformGarbageCollection*"
```

In the original shell, run the following command to generate some load from 20 concurrent clients:

```
$ ab -c 20 -n 1000 http://localhost:3000/users
```

Hit Ctrl+C in the root shell to get the histogram. As you can see, there are _many_ garbage collections, even though they're not taking very long. On one system I tested, the total benchmark time was 22 seconds, and there were over 1800 garbage collections taking from 4 to 7 milliseconds (which accounts for at least 7.2 seconds of execution time, assuming all these collections took 4ms).

- - -

#### Task 3: Identify Call Stacks Causing Heavy Garbage Collection

Finally, in some cases it might not be enough to know that you have a lot of GC; you need to know where the garbage is coming from! Because most GCs are triggered by allocations, it often makes sense to simply sample call stacks for GC start events, and see where in the application they are coming from. To resolve JavaScript symbols, we will need to run the Node process with `--perf_basic_prof`, as before:

```
$ ./run.sh perf
```

In a root shell, run the `stackcount` tool from BCC, attaching to the `gc__start` probe, which is also used by `nodegc`:

```
# stackcount -p $(pgrep -n node) "u:$(which node):gc__start"
```

Run the benchmark again in the original shell to generate some GCs:

```
$ ab -n 100 http://localhost:3000/users
```

And finally, hit Ctrl+C in the root shell to get a stack summary. The heaviest stacks are shown on the bottom, and you can clearly see the paths in the application source that triggered collections. You can repeat the experiment, adding `-f` to the `stackcount` invocation, to get a folded stacks file ready for flame graph generation, if you'd like a better visualization.

- - -

#### Bonus

The [`node-gc-profiler`](https://github.com/bretcope/node-gc-profiler) module can be embedded in your Node application and emit events after each GC cycle, detailing the GC duration and type. You can then use this information to programmatically detect long or excessive GCs. If you have time, try this module out with the `nodey` application and see if you can easily get GC statistics reported.

- - -
