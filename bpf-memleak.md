### Using BPF Tools: Chasing a Memory Leak

In this lab, you will experiment with a C++ application that leaks memory over time, and use the BPF `memleak` tool to determine which allocations are being performed and not being freed. You will do all this without recompiling or even relaunching the leaking application.

- - -

#### Task 1: Ascertain That There Is a Leak

First, build the [wordcount](wordcount.cc) application using the following command:

```
$ g++ -fno-omit-frame-pointer -std=c++11 -g wordcount.cc -o wordcount
```

Run the application. It prompts you for a file name and will display a word count if you provide a valid file name. For example, try the application source file, wordcount.cc. But that's not very interesting. Download one or two books from [Project Gutenberg](http://www.gutenberg.org) -- we like Jane Austen's "Pride and Prejudice", but that's totally up to you. Try giving these as input to the application.

In another shell, run `top` or `htop` and monitor the application's memory usage. When you provide a large file, memory usage goes up and doesn't seem to go down at all. This looks like a memory leak -- some of the data is not being reclaimed between subsequent runs of the tool. 

- - -

#### Task 2: Finding the Leak Source with `memleak`

The `memleak` tool from BCC attaches to memory allocation and deallocation functions (using kprobes or uprobes) and collects data on any allocation that is not being freed. Specifically, for user-mode C++ applications, `memleak` can attach to the `malloc` and `free` functions from libc. For kernel memory leaks, `memleak` can attach to the `kmalloc` and `kfree` functions.

Use the following command to run `memleak` and attach to the word count application:

```
# memleak -p $(pidof wordcount)
```

By default, `memleak` will print the top 10 stacks sorted by oustanding allocations -- that is, allocations performed and not freed since the tool has attached to your process. Try to analyze these call stacks yourself.

> Note that the C++ compiler does not make it very easy to understand what's going on because of name mangling -- you can pipe `memleak`'s output through the `c++filt` utility, which should help. For example: `memleak -p $(pidof wordcount) | stdbuf -oL c++filt`

The most obvious source of allocations is in the `word_counter::word_count` method, which calls `std::copy` to read from the input file. It pushes a bunch of strings into a vector using a `std::back_insert_iterator`. However, it's not obvious why these strings aren't being reclaimed when we move to the next file. What's surprising is that the `std::shared_ptr` to the `word_counter` class, allocated in the `main` function, isn't being freed either. For each file processed, we allocate a
`word_counter` that is not freed. At this point, you could inspect the [wordcount.cc](wordcount.cc) source file carefully and try to determine where the memory leak is coming from.

> Hint: `std::shared_ptr`s do not automatically break cyclic references. Two objects referring to each other through a `std::shared_ptr` will not be automatically reclaimed.

- - -

#### Bonus: Extending `memleak`

Currently, `memleak` is designed to trace *memory* allocations and deallocations. However, there is nothing special about memory -- it is a resource like any other that is allocated and freed in a pair of specific functions. We could use the same approach to trace an arbitrary pair of functions -- `open` and `close` for file descriptions, `sqlite3_open` and `sqlite3_close` for SQLite database handles, and so on.

Take a look at [memleak.py](https://github.com/iovisor/bcc/blob/master/tools/memleak.py) and try to figure out what changes will be required to get `memleak` (or a more general tool) to work with arbitrary pairs of resource-allocating and -freeing functions. Pull requests are always welcome!

- - -

