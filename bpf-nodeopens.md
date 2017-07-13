### Using BPF Tools: Node File Opens

In this lab, you will experiment with a Node application that returns a cryptic error to the client because something goes wrong. You'll investigate the different operations performed by the application prior to issuing the error, and identify the call stack responsible for the faulty operation.

- - -

#### Task 1: Identify Failing Syscalls

Navigate to the `nodey` directory. If you haven't yet, you should make sure all required packages are installed by running `npm install`. Then, run the following command to start the Node application:

```
$ ./run.sh perf
```

Now, run the following command to request the application's About page:

```
$ curl http://localhost:3000/about
```

Instead of an about page, the application returns an error page with no meaningful error details: "An error occurred". In cases like these, you can start chasing through the source code looking for exceptions, or attaching a debugger, but another approach that works in many cases is workload characterization: trying to identify an interaction the application has made with the OS that failed.

In a root shell, run the following command, which uses the `syscount` tool from [BCC](https://github.com/iovisor/bcc), to display any failed syscalls performed by the Node process:

```
# syscount -x -p $(pgrep -n node)
```

Repeat the request to the About page a few times, and then hit Ctrl+C in the root shell. You should see a summary of the failing syscalls; notably, `stat` and `open` are featured multiple times, which leads us to the suspicion that the application might be failing to open a file required for its normal operation, but the error is getting swallowed and a generic message is returned to the client.

- - -

#### Task 2: Snoop Failed File Opens

Now that we know we have failures in `stat` and `open`, let's trace all the failed file opens performed by the Node process when this error occurs. We can use the `opensnoop` tool from BCC for this purpose (run from a root shell):

```
# opensnoop -x -p $(pgrep -n node)
```

In the original shell, repeat the command to request the application's About page.

You should see messages printed by `opensnoop` showing which file the application is trying to open and failing. The error number is also displayed.

- - -

#### Task 3: Identify Stacks Opening Files

The only thing that's left is to try and figure out where the `/etc/nodey.conf` file is being opened. Perhaps you don't recognize this file name, or the application is big enough so that you don't really know which piece of the code might be accessing it. It's time to pull one of the Swiss Army knives BCC has in stock: the `trace` multi-tool, which can attach to arbitrary probe locations and print custom log messages.

Run the following command from a root shell to attach `trace` to the `open` syscall, filtering only to the Node process, and requiring that the first argument of the `open` syscall is the specific string `/etc/nodey.conf` (the `-U` flag requests the userspace call stack when the event occurs):

```
# trace 'SyS_open (STRCMP("/etc/nodey.conf", arg1))' -U -p $(pgrep -n node)
```

Repeat the command to request the application's About page one final time; in the root shell, you should see an output message with a complete userspace stack from the Node process, pointing exactly to the source location which tried to open the requested file.

- - -
