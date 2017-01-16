### Writing BPF Tools: From BCC GitHub Issues

In this lab, you will have the opportunity to contribute to [BCC](https://github.com/iovisor/bcc) development by building tools or taking care of other issues brought up on the project's GitHub repository. The list of issues below is occasionally updated, but some issues might already be closed or reassigned.

Each task has a rough time estimate attached to it, where:

* Short means it's probably possible to finish the task in under an hour.
* Medium means the task requires some design and planning, and will probably take a couple of days to complete.
* Open-Ended means the task requires design, planning, collaboration, and a lot of development work, and will probably take more than a few days to complete.

> NOTE: This list was last updated in October 2016. Some of the tasks below could be closed or addressed already. Please make sure to ask before starting to work on any of them, especially if you are reading this more than a month or two after the last update.

- - -

#### Tool Development Tasks

* [Differentiate pids and tids](https://github.com/iovisor/bcc/issues/547) *Medium*

Update all tools that take a command-line pid or tid argument and that print a pid or tid to make it clear what is being printed. The `bpf_get_current_pid_tgid()` helper returns 64 bits, the lower 32 bits being the tid (`task->pid`). A lot of tools that should really match, filter, or print the pid (`task->tgid`) use this value instead.

* [Use `BPF_PERF_EVENT` Exclusively](https://github.com/iovisor/bcc/issues/540) *Medium*

There are still a couple of tools remaining that print to the trace pipe, which is shared with other Linux tools. Replace any remaining usages of the trace pipe with the `BPF_PERF_OUTPUT` mechanism and move the old versions of the tools to **old/**.

* [`lockstat`](https://github.com/iovisor/bcc/issues/378) *Open-Ended*

This expands on the [`lockstat` lab](bpf-contention.md) to build a full contention monitoring tool with wait graph support.

* [Allow wildcards or regexes in `trace` probe specifiers](https://github.com/iovisor/bcc/issues/746) *Medium*

There are some tools like `stackcount` and `funclatency` that support multiple functions at once by having a wildcard or regex specifier. This can be useful for `trace` as well, e.g. `trace t:block:*` or `trace u:pthread:*mutex*`.

* [Clean up linter issues](https://github.com/iovisor/bcc/issues/745) *Medium*

The tool contribution guidelines require running pep8 and fixing style issues. There are some tools that got a bit neglected over time and have some issues that pep8 detects.

* [Replace `stacksnoop` with `trace -KU`](https://github.com/iovisor/bcc/issues/737) *Short*

Essentially, once stack tracing was introduced in `trace`, there's no longer need for `stacksnoop`. To retain compatibility and discoverability, we can turn `stacksnoop` into a wrapper that invokes `trace` with the appropriate flags.

* [More options in `opensnoop`](https://github.com/iovisor/bcc/issues/616) *Medium*

Bring `opensnoop` up to par with an older version that uses ftrace.

- - -

#### Documentation Tasks

* [man Page Version Update](https://github.com/iovisor/bcc/issues/569) *Short*

Update the tools' man pages to reflect which minimum kernel version is required to run the tool. Notably, tools that rely on perf output buffers require kernel 4.4, tools that rely on stack tracing require kernel 4.6, and tools that rely on BPF support for tracepoints require kernel 4.7.

* [LINKS.md](https://github.com/iovisor/bcc/issues/466) *Short*

Add a collection of links, presentations, blog posts and so on referencing the core BCC project.

* Testing on a New Distribution *Medium*

Find a distribution that isn't covered by the installation requirements, and see if you can get BCC to compile from source on that platform. Document your findings as a PR to the [INSTALL.md](https://github.com/iovisor/bcc/blob/master/INSTALL.md) file or as a separate file linked from INSTALL.md.

- - -

#### Core Development Tasks

* [Higher-Level Language Support](https://github.com/iovisor/bcc/issues/425) *Open-Ended*

Explore building a higher-level language (such as DTrace's language) for expressing BPF probes.

- - -

