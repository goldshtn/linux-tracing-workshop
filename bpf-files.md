### Using BPF Tools: Slow File I/O

In this lab, you will experiment with tracing an application that exhibits latency due to slow I/O operations and will figure out why some of its I/O operations are slower than others.

- - -

#### Task 1: Compile and Run the Application

Run the following command to compile the `logger` application:

```
$ gcc -g -fno-omit-frame-pointer -O0 -pthread logger.c -o logger    # on Ubuntu
$ gcc -g -fno-omit-frame-pointer -O0 -lpthread logger.c -o logger   # on FC
```

Next, run `./logger` -- it sits quietly in the background and churns some log files.

- - -

#### Task 2: Collect I/O Latency Information

For the sake of simplicity, assume you were already told that the `logger` application exhibits occasionally latency. You suspect that this is a result of slow I/O operations. Run `biolatency 1` (a BCC tool) to get a distribution of block I/O operation latency across the system.

The result is likely a bimodal distribution. A lot of the operations complete very quickly, but there are some outliers that take a bit longer. Next, run `biosnoop` (another BCC tool) to see the actual I/O operations and their latencies. You should be able to quickly see that there are some fairly large I/Os performed by the `logger` application that take longer than other smaller I/Os.

- - -

#### Task 3: Observe Slow File I/O Operations

At the time of writing, `biosnoop` does not have filters that let you browse through only slower I/Os. Instead, because we know that `logger` is writing files, we can use the `fileslower` tool from BCC to see which files it is writing, and which operations are taking a bit longer than others:

```
# fileslower 1
```

This should show that `logger` is making occasional 1MB writes to the flush.data file, which are taking longer than the typical 1KB writes to the log.data file.

- - -

