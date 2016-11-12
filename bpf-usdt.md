### Using BPF Tools: Node and JVM USDT Probes

In this lab, you will experiment with discovering USDT probes in a running process or an on-disk executable, enabling these probes at runtime, and collecting them using BPF's USDT support in the `argdist` and `trace` tools.

- - -

#### Task 1: Discovering USDT Probes

The `tplist` tool from the BCC collection can discover USDT probes in an on-disk executable, library, or running process. It can also tell you a lot of details about the probe locations and arguments. Unfortunately, it doesn't tell you the *meaning* of these arguments -- you often have to discover them yourself by using the documentation. In this section, you will walk through this process for the Node.js main executable, `node`.

If you're using an instructor-provided appliance, it should already have Node cloned and configured with USDT support. If you're using your own machine, clone the [Node.js](https://github.com/nodejs/node) repository and build it with the `--with-dtrace` flag, which enables USDT support, using the following commands:

```
$ git clone https://github.com/nodejs/node.git
$ cd node
$ git checkout v6.2.1   # or whatever version is currently stable
$ ./configure --prefix=/opt/node --with-dtrace
$ make -j 3
$ sudo make install
```

Now, you can use `tplist` to discover USDT probes in the Node executable. Run the following command (provide the full paths to `tplist` and `node` as necessary):

```
$ tplist -l node 
```

This should print out a number of USDT probes, including `node:http__server__request` and `node:gc__start`. There are more probes embedded in a typical Node process, however -- you can see exactly what's available (possibly through additional libraries) by running `node` in the background and then using the following command:

```
$ tplist -p $(pidof node)
```

You should now see USDT probes from libc and libstdcxx as well. You can also ask `tplist` to print out more details about each probe. For example, here are the arguments for the `node:http__server_request` probe as reported by `tplist`:

```
$ tplist -l node -vv '*server__request'
/home/vagrant/node/out/Release/node node:http__server__request [sema 0x1606c34]
  location 0xef4854 raw args: 8@%r14 8@%rax 8@-4328(%rbp) -4@-4332(%rbp) 8@-4288(%rbp) 8@-4296(%rbp) -4@-4336(%rbp)
    8 unsigned bytes @ register %r14
    8 unsigned bytes @ register %rax
    8 unsigned bytes @ -4328(%rbp)
    4   signed bytes @ -4332(%rbp)
    8 unsigned bytes @ -4288(%rbp)
    8 unsigned bytes @ -4296(%rbp)
    4   signed bytes @ -4336(%rbp)
```

To figure out what these probes and their arguments mean, we need to inspect the documentation or some authoritative source. In many cases, software instrumented with USDT probes will also ship with a set of files intended for use with SystemTap or DTrace, which can consume these probes. Sure enough, there's a [node.stp](https://github.com/nodejs/node/blob/master/src/node.stp) file that describes these events. Find the description for the
`node:http__server_request` probe:

```
probe node_http_server_request = process("node").mark("http__server__request")
{
    remote = user_string($arg3);
    port = $arg4;
    method = user_string($arg5);
    url = user_string($arg6);
    fd = $arg7;

    probestr = sprintf("%s(remote=%s, port=%d, method=%s, url=%s, fd=%d)",
        $$name, remote, port, method, url, fd);
}
```

It is quite clear what the arguments mean now, and we can use them with the other tracing tools that can retrieve them.

- - -

#### Task 2: Enabling and Tracing Node USDT Probes with `trace`

Let's now make sure that we can trace some HTTP requests with the USDT support embedded in Node. For this experiment, you can build your own Node HTTP server, or just use the [server.js](server.js) file, reproduced here in its entirety:

```javascript
var http = require('http');
var server = http.createServer(function (req, res) {
    res.end('Hello, world!');
});
server.listen(8080);
```

You can run this server using the following command:

```
$ node server.js
```

In a root shell, navigate to the **tools** directory under the BCC source, and use the following command to attach to the `node:http__server__request` USDT event and trace out a message whenever a request arrives. Note that the process id is required because the Node USDT events are not enabled by default. The tracing program must poke a global variable that the probing code reads in order to enable the probe, and that happens on a per-process basis.

```
# trace -p $(pidof node) 'u:/opt/node/node:http__server__request "%s %s", arg5, arg6'
```

Here, `arg5` is going to be the HTTP method and `arg6` is going to be the request URL -- as we discovered by inspecting the .stp file. Finally, make some HTTP requests to your server and make sure you see them in the trace:

```
$ curl localhost:8080
$ curl localhost:8080/index.html
$ curl 'localhost:8080/login?user=dave&pwd=123'
```

- - -

#### Task 3: Enabling and Tracing JVM USDT Probes with `trace`

OpenJDK is also instrumented with a number of USDT probes. Most of them are enabled out of the box, and some must be enabled with a special `-XX:+ExtendedDTraceProbes` flag, because they incur a performance penalty. To explore some of these probes, navigate to your `$JAVA_HOME` and take a look in the **tapset** directory (or online: [tapset](https://github.com/mpujari/systemtap-tapset-openjdk9/tree/master/tapset-1.8.0)):
:

```
$ cd /etc/alternatives/java_sdk
$ ls tapset/
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp
hotspot_gc-1.8.0.77-1.b03.fc22.x86_64.stp
hotspot_jni-1.8.0.77-1.b03.fc22.x86_64.stp
jstack-1.8.0.77-1.b03.fc22.x86_64.stp
```

These .stp files contain descriptions and declarations for a bunch of probes, including their arguments. As an example, try to find the `gc_collect_tenured_begin` and `gc_collect_tenured_end` probe descriptions.

Now, let's move to a more practical example. You are now going to trace a running Java application and see which classes are being loaded, by using the `class_loaded` probe. First, find its "documentation" by running the following command (or by looking online: [hotspot-1.8.0.stp.in](https://github.com/mpujari/systemtap-tapset-openjdk9/blob/master/tapset-1.8.0/hotspot-1.8.0.stp.in#L208)):

```
$ grep -A 10 'probe.*class_loaded' tapset/*.stp
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp:probe hotspot.class_loaded = 
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp-  process("/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.77-1.b03.fc22.x86_64/jre/lib/amd64/server/libjvm.so").mark("class__loaded")                                      
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp-{                           
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp-  name = "class_loaded";    
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp-  class = user_string_n($arg1, $arg2);                                                         
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp-  classloader_id = $arg3;   
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp-  is_shared = $arg4;        
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp-  probestr = sprintf("%s(class='%s',classloader_id=0x%x,is_shared=%d)",                        
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp-                     name, class, classloader_id, is_shared);                                  
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp-}                           
hotspot-1.8.0.77-1.b03.fc22.x86_64.stp-
```

It looks like there are four arguments, and the class name being loaded is given as as the first argument. At this point, we can run a Java application (this is [Slowy.java](slowy/Slowy.java)):

```
$ java slowy/App
```

And now discover the available tracepoints using `tplist`:

```
# jps
31156 App
31398 Jps
# tplist -p 31156 '*class*loaded'
/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.77-1.b03.fc22.x86_64/jre/lib/amd64/server/libjvm.so hotspot:class__unloaded
/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.77-1.b03.fc22.x86_64/jre/lib/amd64/server/libjvm.so hotspot:class__loaded
```

And finally we can trace the interesting tracepoint with `trace` (replace the path to libjvm.so with the appropriate value from your system):

```
# trace 'u:/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.77-1.b03.fc22.x86_64/jre/lib/amd64/server/libjvm.so:class__loaded "%s", arg1'
```

At this point, you should get a trace message whenever a Java app loads a class. For example, let the Slowy app terminate -- are there any classes being loaded on the shutdown path?

- - -

#### Task 4: Using JVM Extended USDT Probes with `argdist`

Finally, let's experiment with one of the extended probes, which isn't always enabled because of its overhead. A couple of these probes are called on method entry and exit, and can be used for simple profiling (albeit with a pretty considerable cost). First, let's find the probes in the .stp file. They are called `method_entry` and `method_return` (or see online: :[`method_entry`](https://github.com/mpujari/systemtap-tapset-openjdk9/blob/master/tapset-1.8.0/hotspot-1.8.0.stp.in#L411) and `method_return`):

```
$ grep -A 10 'probe.*method_entry' *.stp
$ grep -A 10 'probe.*method_return' *.stp
```

As you can see from the results, the class name, method, and signature are available in $arg2, $arg4, and $arg6, respectively. Both probes use the same convention. Now, let's use `argdist` to figure out which methods are being called most often:

```
# argdist -C 'u:/usr/lib/.../libjvm.so:method__entry():char*:arg4' -T 5
```

In another shell, run the Slowy app with the extended probes configured (and disable inlining so that no methods are optimized away):

```
$ java -XX:-Inline -XX:+ExtendedDTraceProbes slowy/App
```

Every five seconds, you should now get a printout of the methods that were called most frequently. It's just the method names, but that's not too shabby. Now, suppose you actually wanted the execution time of each method. That's something `argdist` currently can't handle (it can trace function entry and return, but not probe pairs like we have in this case). However, we can get some basic results with `trace` and post-process them later -- or build a new BCC tool! For now, let's use `trace`
to get all method entry and return events and print them out nicely:

```
# trace -o 'u:/usr/lib/.../libjvm.so:method__entry "%s.%s", arg2, arg4' 'u:/usr/lib/.../libjvm.so:method__return "%s.%s", arg2, arg4'
```

If you run the Slowy app again with the extended probes configured, you should get a bunch of printouts for the methods being entered and exited.

> Unfortunately, you might also see printouts indicating that events are being lost -- we are not printing them quickly enough and the cyclic buffer used for kernel-mode and user-mode communication fills up, discarding some events. This is one of the reasons we're advocating for building custom BPF tools -- they will almost always be more
efficient than trying to grab everything from the kernel and perform aggregations on a large number of events in user space.

- - -

#### Bonus: Discovering Probes in Oracle JVM

So, you think OpenJDK USDT probes are cool? For this bonus task, try to figure out if the Oracle HotSpot VM is also instrumented with USDT probes. See if you can trace some of them with `argdist`, `trace`, or both.

- - -

