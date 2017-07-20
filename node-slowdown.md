### Node Leaky Slowdown

In this lab, you will experiment with a Node application that gradually slows down as more and more requests are made to the process. Over time, this becomes unbearable and administrators are used to forcing a restart; that's not how you roll, though, right?

> NOTE: Unlike other exercises, this exercise is open-ended; we don't provide extraneous clues. It's up to you to use any of the tools from the previous exercises to diagnose this issue. If this is your first lab, perhaps you might want to consider looking at the [Node Profiling with V8](node-prof.md) or [CPU Sampling with `perf` and Flame Graphs](perf.md) labs first.

- - -

#### Task 1: Reproduce the Issue

Navigate to the `nodey` directory. If you haven't yet, you should make sure all required packages are installed by running `npm install`. Then, run the following command to start our simple Node application:

```
$ ./run.sh
```

The application comes with a self-sufficient benchmark that reproduces the issue at hand by issuing 2500 POST requests to the `/position` endpoint, which is supposed to track accurate position information for certain real-world objects reported to the application. Run the following command to start the benchmark:

```
$ ./run.sh bench
```

You will see individual operations reporting their elapsed time. The time begins to degrade, and can reach 2x or worse in only a few seconds (after making a few hundred requests).

- - -

#### Task 2: Diagnose the Problem

From here, you're on your own: profile the application using whatever tools you see fit, until you manage to pinpoint the problem. If you have time, try _fixing_ the problem and running the benchmark again to make sure the issue is really gone.

- - -
