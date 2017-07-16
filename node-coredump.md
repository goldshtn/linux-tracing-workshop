### Node Core Dump Analysis with `llnode`

In this lab, you will experiment with analyzing a core dump of a crashing Node application. Core dump analysis of Node processes used to be a tedious and time-consuming tasks, relying on mdb; this is no longer the case with the introduction of [`llnode`](https://github.com/nodejs/llnode), an LLDB plugin that supports Node core dump analysis.

- - -

#### Task 1: Run the Crashing Application

Navigate to the `nodey` directory. If you haven't yet, you should make sure all required packages are installed by running `npm install`. Then, run the following command to start our simple Node application:

```
$ ./run.sh
```

The process exposes an HTTP PUT endpoint called `/stats`, which expects an authentication key. Try to access the service using the following command, which uses the valid (hard-coded) key:

```
$ curl -X PUT -H 'Content-Type: application/json' -d '{ "auth": "mykey" }' http://localhost:3000/stats
```

You should get a 201 (Created) response back. But what if you provide the wrong key? This is something that would probably not happen most of the time, but in a mis-configured system or when the service is being probed from the Internet, this might happen; run the following command to see what happens then:

```
$ curl -X PUT -H 'Content-Type: application/json' -d '{ "auth": "mykey123" }' http://localhost:3000/stats
```

It seems that the Node process crashes in flames! This isn't good, and the way things are configured right now, it simply disappears without a trace.

- - -

#### Task 2: Capture a Core Dump of the Crash

To diagnose the problem, run the following commands, which set up core dump generation and remove the core file size limit:

```
$ ulimit -c unlimited
$ sudo bash -c 'echo core > /proc/sys/kernel/core_pattern'
```

Additionally, the Node process needs to be started with the `--abort-on-uncaught-exception` flag, so that an unhandled exception ends up crashing the process and generating the core dump. Run the following command to enable it using the wrapper script:

```
$ ./run.sh core
```

And now reproduce the crash by accessing the `/stats` endpoint again with an invalid key:

```
$ curl -X PUT -H 'Content-Type: application/json' -d '{ "auth": "mykey123" }' http://localhost:3000/stats
```

This time, when the process crashes, a core file should be generated in the current directory. This is the file we will now analyze.

- - -

#### Task 3: Analyze the Crash in LLDB

Now that we have the core dump, we can open it in LLDB and use the `llnode` plugin. The `llnode` plugin can be installed separately using `npm install -g llnode`; in the instructor-provided VM, it will be in `~/tracing-workshop/node_modules/.bin/llnode`. You can run it as follows, to provide the core file to load:

```
$ ~/tracing-workshop/node_modules/.bin/llnode -c core.1234
```

When LLDB initializes, it will print basic information about the crash. Try running the `bt` command, which displays a stack backtrace. You can clearly see the native Node functions, but not the JavaScript frames -- these are not resolved, because LLDB doesn't have built-in support for them. Run the following command to see the actual stack with JavaScript frames resolved:

```
(lldb) v8 bt
```

This points clearly to the [index.js](nodey/routes/index.js) file as the culprit, showing an anonymous function that is getting invoked as part of a timer. The two anonymous frames in index.js have `fn=0xnnnn...` tacked to the end; you can pass these addresses to the `v8 inspect` command to see more information about these anonymous functions. This should lead you to the source of the crash; make sure you can see the `key` parameter passed to the authentication function, which you'd be
able to use to figure out the wrong key provided by the client (if you hadn't run the client yourself just a few minutes ago).

- - -

#### Bonus

The [`core-dump`](https://github.com/davidmarkclements/core-dump) Node module can be used for somewhat more advanced core dump generation. For example, you can use it to generate core dumps programmatically from within the Node process (e.g. when an interesting condition is met), or you can use it to generate a core dump externally from a shell. (The latter can also be done with `gcore`; see the [Node Memory Leak Analysis exercise](node-memleak.md).)

- - -
