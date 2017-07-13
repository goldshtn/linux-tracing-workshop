### Node Profiling with V8

In this lab, you will experiment with profiling a CPU bottleneck in a Node.js application using the built-in V8 profiler. The key advantage of using the built-in profiler is that it is, well, built-in: you don't need any additional software, and it works across all platforms. On the other hand, it requires that you restart your Node application with a special profiling switch, which you can't leave on for a long duration of time.

- - -

#### Task 1: Run the Application with a Benchmark

Navigate to the `nodey` directory. If you haven't yet, you should make sure all required packages are installed by running `npm install`. Then, run the following command to start our simple Node application:

```
$ ./run.sh
```

This starts the web server asynchronously. One of its endpoints is a simple authentication service, which takes a username and password and returns user information. You can try it with curl:

```
$ curl -X POST 'http://localhost:3000/users/auth?username=foo&password=wrong'
```

Let's ignore for a second the horrible practice of passing the username and password as part of the query string, and focus on the performance of this endpoint. Run the following command to get a simple local benchmark of its behavior:

```
$ ab -c 10 -n 100 -m POST 'http://localhost:3000/users/auth?username=foo&password=wrong'
```

In another shell, you can run `top` to get a quick idea of the system's behavior. You can clearly see the Node process consuming lots of CPU. The benchmark results are not very optimistic, either; the mean time to service an authentication request is quite high.

- - -

#### Task 2: Run the Application with the V8 Profiler

To profile the application, we will use the built-in V8 profiler. The wrapper script, [run.sh](nodey/run.sh), can invoke the Node runtime with the `--prof` switch, which turns on the profiler. The profiler output goes to a file named `isolate-nnnnn.log`, which can then be parsed and analyzed.

Run the application with the profiler enabled:

```
$ ./run.sh prof
```

Run the benchmark again:

```
$ ab -c 10 -n 100 -m POST 'http://localhost:3000/users/auth?username=foo&password=wrong'
```

Now, look in the current directory for the `isolate-nnnnn.log` file. You don't have to parse it by hand; you can ask the Node runtime to parse it and produce a text report:


```
$ node --prof-process isolate*.log | less
```

Inspect the report and try to answer the following questions:

* Which JavaScript method was responsible for most of the CPU usage?
* What's the proportion of time spent in JavaScript code, C++ code, or GC code?
* Which call stack leads to the most CPU-consuming function?

- - -

