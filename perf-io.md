### Using `perf` Tools: Slow File I/O

In this lab, you will experiment with tracing an application that exhibits latency due to slow I/O operations. You will also collect call stacks to determine where the I/O operations are coming from, and visualize them using a flame graph.

There are two applications that you can pick from: a C program and a Java program. For the C program, perform tasks 1-3 below. For the Java program, perform tasks 4-6.

- - -

#### Task 1: Compile and Run the C Application

Run the following command to compile the `logger` application:

```
$ gcc -g -fno-omit-frame-pointer -O0 -pthread logger.c -o logger    # on Ubuntu
$ gcc -g -fno-omit-frame-pointer -O0 -lpthread logger.c -o logger   # on FC
```

Next, run `./logger` -- it sits quietly in the background and churns some log files.

> Note: Although specifying `-fno-omit-frame-pointer` is redundant when optimizations are turned off, it's a good habit to compile with this flag whenever you're going to use tracing tools.

- - -

#### Task 2: Collect I/O Size and Latency Information

Next, you are going to collect I/O size and latency information using two tools from the [perf-tools](https://github.com/brendangregg/perf-tools) toolkit. First, run `iolatency` for a few seconds and then hit Ctrl+C to get a histogram of I/O operation latencies on the system. Most I/Os should probably be pretty quick, but it really depends on your disk speed. Here's an example of what it can look like:

```
# ./iolatency
Tracing block I/O. Output every 1 seconds. Ctrl-C to end.

  >=(ms) .. <(ms)   : I/O      |Distribution                          |
       0 -> 1       : 186      |######################################|
       1 -> 2       : 8        |##                                    |
       2 -> 4       : 5        |##                                    |

  >=(ms) .. <(ms)   : I/O      |Distribution                          |
       0 -> 1       : 188      |######################################|
       1 -> 2       : 11       |###                                   |
       2 -> 4       : 5        |##                                    |
       4 -> 8       : 1        |#                                     |

^C
Ending tracing...
```

To characterize the I/O operations a bit more, run `bitesize` and hit Ctrl+C after a few seconds to get a picture of block I/O operation sizes:

```
# ./bitesize
Tracing block I/O size (bytes), until Ctrl-C...

            Kbytes         : I/O      Distribution
              -> 0.9       : 542      |######################################|
          1.0 -> 7.9       : 518      |##################################### |
          8.0 -> 63.9      : 208      |###############                       |
         64.0 -> 127.9     : 2        |#                                     |
        128.0 ->           : 53       |####                                  |
^C
```

From these results, it looks like there are a lot of small I/Os, including very small ones (under 1KB), but there is also a non-negligible number of I/Os that are larger than 128KB, which isn't very small. In the next section, we will discover where these I/Os are coming from in the application's code.

- - -

#### Task 3: Collect Block I/O Call Stacks and Flame Graph

To identify block I/O operation sources, we are going to record the `block:block_rq_insert` kernel tracepoint, including user-space call stacks, and correlate them to our application's behavior. To do so, run the following command (while `./logger` is still executing):

```
# perf record -p $(pidof logger) -e block:block_rq_insert -g -- sleep 10
```

After 10 seconds, perf will exit and inform you how many samples were collected. Each sample represents a block I/O operation, and includes a call stack. To analyze these call stacks, run `perf report --stdio` -- the results should point to functions in the logger binary such as `logger` and `write_flushed_data`.

It always helps to visualize stack trace information as a flame graph. Run the following command to generate a flame graph SVG, and then open it in a browser or some other SVG viewer to see the stack traces visually (in the following command, replace `FlameGraph` if needed with the directory where you cloned the [FlameGraph](https://github.com/brendangregg/FlameGraph) repository):

```
# perf script | FlameGraph/stackcollapse-perf.pl | FlameGraph/flamegraph.pl > io-stacks.svg
```

This is it -- you now found the source of the disk I/O operations in this application. If you want to repeat the same experiment with a Java program, continue to the next task.

- - -

#### Task 4: Compile and Run the Java Application

Navigate to the `buggy` directory in the workshop repository, and then run the following command to build and run the Java application:

```
$ make writey
```

- - -

#### Task 5: Collect I/O Size and Latency Information

Next, you are going to collect I/O size and latency information using two tools from the [perf-tools](https://github.com/brendangregg/perf-tools) toolkit. First, run `iolatency` for a few seconds and then hit Ctrl+C to get a histogram of I/O operation latencies on the system. Most I/Os should probably be pretty quick, but it really depends on your disk speed. Here's an example of what it can look like:

```
# ./iolatency
Tracing block I/O. Output every 1 seconds. Ctrl-C to end.

  >=(ms) .. <(ms)   : I/O      |Distribution                          |
       0 -> 1       : 9        |######################################|
       1 -> 2       : 0        |                                      |
       2 -> 4       : 2        |#########                             |
       4 -> 8       : 2        |#########                             |

  >=(ms) .. <(ms)   : I/O      |Distribution                          |
       0 -> 1       : 4        |######################################|
       1 -> 2       : 1        |##########                            |
       2 -> 4       : 4        |######################################|

^C
Ending tracing...
```

To characterize the I/O operations a bit more, run `bitesize` and hit Ctrl+C after a few seconds to get a picture of block I/O operation sizes:

```
# ./bitesize
Tracing block I/O size (bytes), until Ctrl-C...

            Kbytes         : I/O      Distribution
              -> 0.9       : 2        |#                                     |
          1.0 -> 7.9       : 76       |######################################|
          8.0 -> 63.9      : 0        |                                      |
         64.0 -> 127.9     : 0        |                                      |
        128.0 ->           : 19       |##########                            |
^C
```

From these results, it looks like there are a lot of small I/Os, between 1 and 8 kilobytes in size, but there is also a non-negligible number of I/Os that are larger than 128KB, which isn't very small. In the next section, we will discover where these I/Os are coming from in the application's code.

- - -

#### Task 6: Collect Block I/O Call Stacks and Flame Graph

Because this is a Java program, `perf` will need access to symbol information that is not available by default. Run the following command from the [perf-map-agent](https://github.com/jrudolph/perf-map-agent) repository root to create a perf map file for the Java process:

```
$ perf-map-agent/bin/create-java-perf-map.sh $(pidof java)
```

> Note: the previous step should be run under the same user you launched the Java application with. It will not work if you launched the Java application as a standard user, and then ran the `create-java-perf-map.sh` script as root.

To identify block I/O operation sources, we are going to record the `block:block_rq_insert` kernel tracepoint, including user-space call stacks, and correlate them to our application's behavior. To do so, run the following command:

```
# perf record -p $(pidof java) -e block:block_rq_insert -g -- sleep 10
```

After 10 seconds, perf will exit and inform you how many samples were collected. Each sample represents a block I/O operation, and includes a call stack. To analyze these call stacks, run `perf report --stdio` -- the results should point to functions in the Java program such as `Writey::flushData` and `Writey::writer`.

It always helps to visualize stack trace information as a flame graph. Run the following command to generate a flame graph SVG, and then open it in a browser or some other SVG viewer to see the stack traces visually (in the following command, replace `FlameGraph` if needed with the directory where you cloned the [FlameGraph](https://github.com/brendangregg/FlameGraph) repository):

```
perf script | FlameGraph/stackcollapse-perf.pl | FlameGraph/flamegraph.pl --color java > io-stacks.svg
```

> Note: The `--color java` argument changes the default color palette for the flame graph to differentiate between C frames, native JVM frames, and Java frames.

This is it -- you now found the source of the disk I/O operations in this application.

- - -
