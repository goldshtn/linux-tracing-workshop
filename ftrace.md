### Probing Tracepoints with ftrace

In this lab, you will experiment with using ftrace directly (through debugfs) for enabling a kernel tracepoint and reviewing the trace results. You will also experiment with setting filters and with the general-purpose function tracer.

- - -

#### Task 1: Enable the `sched:sched_switch` Tracepoint

If you suspect that some component on the system is burning CPU cycles by switching threads too frequently, you can figure out who is responsible by using the `sched:sched_switch` tracepoint. This tracepoint provides information about the process being switched out as well as the process being switched in.

From a root shell, cd to the **/sys/kernel/debug/tracing** directory:

```
# cd /sys/kernel/debug/tracing
```

Enable tracing if it's not already enabled:

```
# echo 1 > tracing_on
```

Review the format of the `sched:sched_switch` tracepoint using the following command:

```
# cat events/sched/sched_switch/format
```

Now, enable the tracepoint:

```
# echo 1 > events/sched/sched_switch/enable
```

Wait a few seconds (optionally launching some processes that compete for CPU time), and then inspect the trace file:

```
# cat trace
```

Make sure you understand the format of the output. Specifically, which process is being switched out and which process is being switched in. It is fairly easy to build a tool that would post-process the trace output file and aggregate some statistics for you. Unfortunately, this kind of post-processing would have to happen in user-space, after the trace events have already been emitted.

Finally, disable the tracepoint:

```
# echo 0 > events/sched/sched_switch/enable
```

#### Task 2: Enable the Function Tracer

If you need to understand what's happening during the invocation of a particular kernel function, how often it's called, or how long it takes to run, you can use the `function` or `function_graph` tracers. These are restricted to kernel functions only, but can be very useful.

Make sure you are still in the **/sys/kernel/debug/tracing** directory. Enable the function tracer:

```
# echo function > current_tracer
```

Make `vfs_write` the function you want traced:

```
# echo vfs_write > set_ftrace_filter
```

Inspect the trace file. There's not a lot of information for each invocation, so you might also care about what's going on inside the `vfs_write` function. That's what the `function_graph` tracer is used for:

```
# echo function_graph > current_tracer
# echo > set_ftrace_filter
# echo vfs_write > set_graph_function
```

Take a look at the trace file again. This time, each invocation of `vfs_write` is traced in a detailed format so you can understand exactly which other methods were invoked and how long they took. Again, some post-processing of this data might be in order to create a more accurate, visual illustration of the flow graph inside the function. If you want to restrict the depth of the graph, use:

```
# echo 2 > max_graph_depth
```

Finally, disable the tracer:

```
# echo nop > current_tracer
# echo > set_graph_function
```

- - -

#### Bonus: Setting User-Mode Probes with ftrace

Figure out how to set user-mode probes (uprobes) with ftrace. Note that ftrace does not natively understand user-space function names; you will need to provide a binary and an offset. To discover these, you'll need `objdump` or a similar tool.

Configure ftrace to trace all calls to the libc `write` function across all processes on the system. Try to add an additional filter that would keep only writes to file descriptor 1 (traditionally stdout).

- - -
