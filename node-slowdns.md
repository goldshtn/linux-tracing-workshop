### Node Slow DNS

In this lab, you will experiment with a Node service that talks to external HTTP endpoints (think additional microservices) and occasionally returns a slow result. This time, we will focus on investigating the DNS queries performed by the application, and see what it looks like when the DNS server is slow to respond, or fails to produce a result.

- - -

#### Task 1: Run the Application

Navigate to the `nodey` directory. If you haven't yet, you should make sure all required packages are installed by running `npm install`. Then, run the following command to start our simple Node application:

```
$ ./run.sh dns
```

Run a couple of queries to the Inventory endpoint by using the following commands. Note the time it takes to return a response from the service.

```
$ time curl http://localhost:3000/inventory?product_id=a19224
$ time curl http://localhost:3000/inventory?product_id=a19877
$ time curl http://localhost:3000/inventory?product_id=a88711
```

The service seems to working quite slowly, and the same products that worked all right in the [HTTP exercise](node-slowhttp.md) are now fetching very slowly. Add a different request into the mix: a request for a "dynamic" product, where the inventory service endpoint is generated dynamically:

```
$ time curl http://localhost:3000/inventory?product_id=dyn88711
```

The result is even slower to arrive, and there's also a generic error that shows up in the response document.

- - -

#### Task 2: Inspect DNS Resolution Requests

In some cases, a DNS mis-configuration can be responsible for very poor HTTP request latency. The `gethostlatency` tool from [BCC](https://github.com/iovisor/bcc) is designed to inspect DNS resolution requests and their latencies. Run it as follows, from a root shell:

```
# gethostlatency
```

If you repeat one of the accesses to the Inventory endpoint, you'll see the application makes multiple DNS requests to resolve its backend service hostnames. Node.js doesn't cache DNS responses (by design), which means we keep issuing these DNS resolution queries on each HTTP request. It still doesn't explain why the DNS resolution is slow, but we can confirm that the DNS resolution time is a considerable portion of the overall slowdown our HTTP queries are experiencing.

To understand the delays in processing our DNS query, it helps to take a look at the DNS servers configured on the machine:

```
$ cat /etc/resolv.conf
```

Looks like there is a local DNS server, which takes precedence to everything else. Now we can use the `dig` tool to query that specific DNS server and see its behavior compared to a public Internet nameserver (`dig` is in the dnsutils package on Ubuntu, or the bind-utils package on Fedora):

```
$ dig google.com @127.0.0.1
$ dig google.com @8.8.8.8
```

The local query is very slow! So it looks like we have a rogue local DNS server on our hands. Indeed, the [`slodns`](https://github.com/goldshtn/slodns) tool is running in the background and slowing down our DNS queries. Kill it with the following command:

```
$ ./run.sh killdns
```

Now retry the Inventory endpoint accesses. They should be much faster, and if you look at what `gethostlatency` has to say, you'll see the DNS queries are resolved more rapidly.

- - -

#### Bonus

Do you think it's normal that the Node process is making a DNS query for each outgoing HTTP request? Try to look online to find a reasonable solution for this problem that would make sense in your environment.

- - -
