### Writing BPF Tools: `setuidsnoop`

In this lab, you will modify the existing [`killsnoop`](https://github.com/iovisor/bcc/tree/master/tools/killsnoop.py) tool to trace `setuid` calls, which include SSH logins, sudo, su, and other potentially interesting activity.

- - -

#### Task 1: Inspect `killsnoop.py`

Navigate to the [`killsnoop.py`](https://github.com/iovisor/bcc/tree/master/tools/killsnoop.py) file and inspect the source code. This is a simple kprobe-based tool that attaches a kprobe and a kretprobe to the `sys_kill` function. In the enter probe, data about the kill signal and target process is recorded, and in the return probe, the result of the operation is recorded and a custom structure is submitted to user space for display.

The Python script mostly deals with parsing arguments and taking care of the BPF program (e.g., by embedding a filter that makes sure only signals to a specific process are traced), and then polls the kernel buffer for results and prints them as they arrive.

Try running `killsnoop` in one shell, and killing some process (e.g. with `kill -9`) in another shell to see what the tool's output looks like.

- - -

#### Task 2: Approximating `setuidsnoop` with `trace`

For lack of a dedicated tool, we can use the general-purpose `trace` multitool to trace `setuid` calls by attaching two probes:

```
# trace 'sys_setuid "uid=0x%x", arg1' 'r::sys_setuid "rc=%d", retval'
```

In a separate shell, run `sudo su` or a similar command, and note the trace printouts when `setuid` is called.`

- - -

#### Task 3: Developing `setuidsnoop`

A dedicated tool can be better than the trace printouts. For one thing, it would consolidate the `setuid` argument and return code, much like `killsnoop` does. Copy `killsnoop.py` to `setuidsnoop.py` and make the following changes:

* Attach the kprobe and kretprobe to `sys_setuid` instead of `sys_kill`
* Modify the signature of the kprobe function to match `sys_setuid`
* Modify the data structure to include the uid instead of the kill arguments (both the C and Python structures need to be modified)
* Populate the data structure accordingly in the kprobe and kretprobe functions
* Modify the printing code on the Python side to output the uid and result

Finally, test your tool by running `sudo su` or a similar command.

- - -

