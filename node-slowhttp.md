### Node Slow HTTP Requests

In this lab, you will experiment with a Node service that talks to external HTTP endpoints (think additional microservices) and occasionally returns a slow result. Although there are third-party APM solutions that can help diagnose this kind of issue, we will use the USDT probes built into Node, which don't require any code changes or restarting the service.

- - -

#### Task 1: Run the Application

Navigate to the `nodey` directory. If you haven't yet, you should make sure all required packages are installed by running `npm install`. Then, run the following command to start our simple Node application:

```
$ ./run.sh
```

Run a couple of queries to the Inventory endpoint by using the following commands. Note the time it takes to return a response from the service.

```
$ time curl http://localhost:3000/inventory?product_id=a19224
$ time curl http://localhost:3000/inventory?product_id=a19877
$ time curl http://localhost:3000/inventory?product_id=a88711
```

Depending on how far you are from the inventory service's backend, these queries may return in under a second or take a few full seconds. However, this is the baseline, and most requests seem to take the same time.

Occasionally, though, we have reports of slow requests. After some spelunking, this is how to make a slow request:

```
$ time curl http://localhost:3000/inventory?product_id=g11899
```

What's wrong with this specific product? It seems to be taking much slower than the other calls, in a fairly reliable way.

- - -

#### Task 2: Inspect Outgoing HTTP Requests with `trace` and `nhttpslower`

It might be interesting to explore the outgoing HTTP requests that our Node service makes to the inventory service on our behalf. Recall that our build of Node is instrumented with USDT probes, including probes for HTTP requests; this means we can use the `trace` tool from [BCC](https://github.com/iovisor/bcc) to trace outgoing HTTP requests. In a root shell, run the following command to do so:

```
# trace -p $(pgrep -n node) 'u::http__client__request "%s %s", arg5, arg6'
```

Now, in the original shell, access the Inventory endpoint again. You should see printouts from `trace` indicating which backend services are being invoked. It looks like there are five different backend services that our Node application is talking to. But how long are these requests taking?

The [`nhttpslower`](nhttpslower.py) tool is a demo tool that traces the duration of Node HTTP requests (incoming and outgoing), using the USDT probes built into Node. It can be very useful in our case; run it like this, from a root shell:

```
# ./nhttpslower.py $(pgrep -n node) --client
```

> The `--client` switch instructs the tool to print only HTTP requests our Node application makes to the outside world (as a client), and not any incoming HTTP requests. This is just to avoid cluttering the output.

Now, access the Inventory endpoint again. `nhttpslower` should report the exact endpoints accessed, the timestamps when the operation was issued, and its duration. Immediately, the slow backend service becomes apparent: it's the `g11` service. It also becomes apparent that the application is issuing the inventory requests serially: it waits for each request to complete before issuing the next one.

```
# ./nhttpslower.py $(pgrep -n node) --client
Snooping HTTP requests in Node process 14248, Ctrl+C to quit.
TIME_s         TYP DURATION_ms REMOTE           PORT  METHOD   URL
0.000000000    CLI     870.875 undefined        0     GET      /get?svc=a12&inventory=available&product_id=g1231
0.873364421    CLI     639.769 undefined        0     GET      /get?svc=b22&inventory=available&product_id=g1231
1.515958655    CLI    2668.286 undefined        0     GET      /delay/2?svc=g11&inventory=not_available&product_id=g1231
4.188127138    CLI     642.796 undefined        0     GET      /get?svc=a78&inventory=not_available&product_id=g1231
4.833378060    CLI     633.348 undefined        0     GET      /get?svc=b43&inventory=available&product_id=g1231
```

We might not be able to fix the `g11` service, but we can definitely fix the fact we're making the requests serially. You can open the [index.js](nodey/routes/index.js) file and modify the `/inventory` route to use `async.map` instead of `async.mapSeries`. This should immediately improve the client's performance, because we're not waiting for each backend service to return before calling the next one.

- - -

#### Bonus

Alternative approaches for tracing request latency are more invasive. Some options you could try include:

* Hooking the `request` function with something like [`request-debug`](https://github.com/request/request-debug), and measuring the latency yourself.

* Using a local proxy to pipe your requests through, and then inspecting them in the proxy's interface.

* Using `tcpdump` or another sniffer to record the traffic and analyze it later. With something like `tshark`, you can get HTTP latency summaries and perform various aggregations. (Technically, this can be done with a BPF program in real-time, too; it's just that someone has to write a BPF program to parse HTTP requests and responses.)

Here's an example of a tshark command that captures packets and displays a summary of HTTP requests by HTTP host and URL:

```
# tshark -z http_req,tree -Y http
```

- - -

