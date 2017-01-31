### Writing BPF Tools: Contention Stats and Stacks

In this lab, you will experiment with what it takes to write your own BPF-based tools using BCC. You will write C and Python code that analyzes lock acquire and release operations and produces statistics on locks and threads. Although the resulting tool is not going to be ready for production use, it should demonstrate the basic requirements for custom BPF tools.

- - -

#### Task 1: Build and Run the Concurrent Application

First, you'll build a multi-threaded application ([parprimes.c](parprimes.c)) that calculates -- guess what -- prime numbers. It does so in multiple threads and uses a mutex, which makes it a good candidate for our purposes. Run the following command to build it:

```
$ gcc -g -fno-omit-frame-pointer -lpthread parprimes.c -o parprimes     # on Fedora
$ gcc -g -fno-omit-frame-pointer -pthread  parprimes.c -o parprimes     # on Ubuntu
```

Try to run it with the following parameters, just to see that it works:

```
$ ./parprimes 4 10000
```

It should spawn 4 threads, and each thread should print how many primes it was able to find.

- - -

#### Task 2: Implement `lockstat`, a Contention Monitoring Tool

Next, you are going to fill in bits and pieces of [lockstat.py](lockstat.py), a contention monitoring tool based on BCC and uprobes. There's quite a bit of code to take in, so if you'd rather inspect and run a completed solution, it's also available as [lockstat-solution.py](lockstat-solution.py).

The general idea is to probe multiple functions that have to do with mutexes (and potentially other synchronization mechanisms, in the future). For pthread mutexes, `pthread_mutex_lock` and `pthread_mutex_unlock` are the functions for acquiring and releasing the lock, and `pthread_mutex_init` is the function that initializes a mutex. By probing these functions, you will collect the following information:

* The initialization stack trace for each mutex, which can be used to identify it later
* The time each thread spent waiting for each mutex in the program, and the call stack where it waited
* The time each thread spent inside each mutex in the program

By aggregating this information, you will be able to pinpoint the contention points in the program: mutexes that become a bottleneck because many threads spend time waiting for them. You will also be able to determine which threads spend a lot of time holding these mutexes, thereby preventing other threads from making progress.

Follow the `TODO` comments in the [lockstat.py](lockstat.py) file and fill in pieces of code as necessary. Specifically, you will fill in code that updates BPF hashes and histograms (on the C/BPF, kernel-mode side) and code that attaches probes and reads these data structures (on the Python, user-mode side).

- - -

#### Task 3: Monitor Lock Contention with `lockstat`

Run the following command to keep a bunch of threads busy for a while:

```
$ ./parprimes 4 10000000
```

In a root shell, run your `lockstat` script and observe the results. Note that the lock wait and hold times are not distributed evenly across the threads. Do you understand why? What's the distribution of wait and hold times like? Are there are any long wait times, or long hold times? A long wait time could be caused by contention on the lock, but what could cause a long hold time, considering that the only operation under the lock is incrementing an integer?

> In this particular example, the lock acquire and release operations are entirely artificial because the `count` variable they protect is actually private to a single thread.

- - -

#### Bonus: Add Extra Features to `lockstat`

Here are some extra features `lockstat` needs to become closer to production use. You can pick any feature and work on it later. Pull requests are welcome! :-)

* Print filtered information, e.g. only mutexes for which the aggregated wait time was more than N ms
* Support additional synchronization mechanisms, other than a pthread mutex
* Collect average, min, max, and possibly even a histogram of wait times and hold times per mutex and per thread
* Perform sampling to reduce overhead -- collect only N% of the events

- - -

