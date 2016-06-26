### Writing BPF Tools: From BCC GitHub Issues

In this lab, you will have the opportunity to contribute to [BCC](https://github.com/iovisor/bcc) development by building tools or taking care of other issues brought up on the project's GitHub repository. The list of issues below is occasionally updated, but some issues might already be closed or reassigned.

Each task has a rough time estimate attached to it, where:

* Short means it's probably possible to finish the task in under an hour.
* Medium means the task requires some design and planning, and will probably take a couple of days to complete.
* Open-Ended means the task requires design, planning, collaboration, and a lot of development work, and will probably take more than a few days to complete.

- - -

#### Tool Development Tasks

* [Differentiate pids and tids](https://github.com/iovisor/bcc/issues/547) *Medium*

Update all tools that take a command-line pid or tid argument and that print a pid or tid to make it clear what is being printed. The `bpf_get_current_pid_tgid()` helper returns 64 bits, the lower 32 bits being the tid (`task->pid`). A lot of tools that should really match, filter, or print the pid (`task->tgid`) use this value instead.

* [Use `BPF_PERF_EVENT` Exclusively](https://github.com/iovisor/bcc/issues/540) *Medium*

There are still a couple of tools remaining that print to the trace pipe, which is shared with other Linux tools. Replace any remaining usages of the trace pipe with the `BPF_PERF_OUTPUT` mechanism and move the old versions of the tools to **old/**.

* [`offcpudist`](https://github.com/iovisor/bcc/issues/500) *Medium*

This is a tool that would print histograms of blocked time by process, possibly with stack traces as well. It should be based on the `sched:sched_switch` tracepoint.

* [`cpudist`](https://github.com/iovisor/bcc/issues/499) *Medium*

This is a tool that would print histograms of on-CPU time by process, possibly with stack traces as well. It should be based on the `sched:sched_switch` tracepoint.

* [`lockstat`](https://github.com/iovisor/bcc/issues/378) *Open-Ended*

This expands on the [`lockstat` lab](bpf-contention.md) to build a full contention monitoring tool with wait graph support.

- - -

#### Documentation Tasks

* [man Page Version Update](https://github.com/iovisor/bcc/issues/569) *Short*

Update the tools' man pages to reflect which minimum kernel version is required to run the tool.

* [LINKS.md](https://github.com/iovisor/bcc/issues/466) *Short*

Add a collection of links, presentations, blog posts and so on referencing the core BCC project.

* [Reference Guide](https://github.com/iovisor/bcc/issues/465) *Medium*

Add a reference guide that documents the `BPF` module and its sub-modules.

* [Kernel Requirements](https://github.com/iovisor/bcc/issues/464) *Short*

Which kernel build flags are required to use the BPF-based tools? Which kernel version supports which features, for which tools?

* Testing on a New Distribution *Medium*

Find a distribution that isn't covered by the installation requirements, and see if you can get BCC to compile from source on that platform. Document your findings as a PR to the [INSTALL.md](https://github.com/iovisor/bcc/blob/master/INSTALL.md) file or as a separate file linked from INSTALL.md.

- - -

#### Core Development Tasks

* [Executable File Path Resolution](https://github.com/iovisor/bcc/issues/565) *Short*

Update BCC to allow tools like `trace` and `argdist` to specify an executable name which is in the PATH and get it resolved automatically.

* [Higher-Level Language Support](https://github.com/iovisor/bcc/issues/425) *Open-Ended*

Explore building a higher-level language (such as DTrace's language) for expressing BPF probes.

* [Regex Support For UProbes](https://github.com/iovisor/bcc/issues/389) *Short*

Allow using a regular expression for the function name when using `attach_uprobe`.

- - -
