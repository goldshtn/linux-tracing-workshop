### Using BPF Tools: Node MySQL Slow Queries

In this lab, you will experiment with a Node application that performs MySQL database queries, and some of these queries are taking an unreasonably long time. You will trace slow queries on the database side, and also learn how to trace specific query stacks on the Node side.

- - -

#### Task 1: Running the Slow Application

Prepare the MySQL database process by running it first. If you're using the instructor-provided environment, you can launch MySQL using the following command:

```
$ sudo -u mysql /usr/local/mysql/bin/mysqld_safe --user=mysql &
```

Then, prepare the database using the [`mysql-db.sh`](mysql-db.sh) script:

```
$ ./mysql-db.sh
```

Navigate to the `nodey` directory. If you haven't yet, you should make sure all required packages are installed by running `npm install`. Then, run the following command to start the Node application:

```
$ ./run.sh
```

Now that the application is started, run the following command to query the set of products from the database:

```
$ curl http://localhost:3000/products
```

You should see a JSON document dumping all the products and descriptions. This is great, but feels a bit sluggish; run the following command to benchmark this operation as it is executed by multiple concurrent clients:

```
$ ab -c 5 -n 20 http://localhost:3000/products
```

It takes about 3 seconds on average on one of my test machines to return each product listing. And that sounds extraneous, considering the small size of the output set.

- - -

#### Task 2: Tracing Queries on the Database Side

The `dbslower` tool from [BCC](https://github.com/iovisor/bcc) recognizes MySQL and PostgreSQL, and can trace query executions in general, and specifically queries slower than a given threshold.

> NOTE: For MySQL, the `dbslower` tool was recently updated with support for tracing the database even if it wasn't built with USDT support, by using uprobes directly. For PostgreSQL, only USDT support is available at the time of writing.

Run the following command to trace all database queries to the MySQL database (from a root shell):

```
# dbslower -p $(pidof mysqld) mysql -m 0
```

In the original shell, make another trip to the Products page. The root shell should then show the queries executed by the database. It might be a bit hard to see the slow query right away, so you can re-run `dbslower` with a larger threshold, e.g. `dbslower -p $(pidof mysqld) mysql -m 500` to trace only queries slower than 500ms.

This shows that the `CALL getproduct(97)` query is very slow, taking more than 2 seconds, and much slower than any other query. And it's a bit odd: what's so special about number 97, compared to, say, 96, or 98? Turns out, we can trace database queries on a slightly lower level, by using the USDT probes directly with the `trace` tool from BCC:

```
# trace -p $(pidof mysqld) 'u::query__exec__start "%s", arg1'
```

If you make another trip to the Products page now, and then look at the executed queries, you'll notice that there are additional commands the database performs, which were not in the `dbslower` output. Notably, the `SLEEP` call suddenly shows up, and explains the 2 second delays in executing the `getproduct` call!

- - -

#### Task 3: Tracing Queries on the Application Side

At this point, we have reasonably good clarity on the database side as to the queries being executed. In some cases, though, you might not have access to the database server, and need to perform this kind of tracing on the application side. Although there are APM solutions that can trace database queries from Node.js, there is nothing built-in. We will use a demo tool called [`mysqlsniff`](mysqlsniff.py), which sniffs the MySQL protocol on the socket layer and prints queries matching a given
prefix; it can even include call stacks!

In a root shell, run the following command from the main labs directory:

```
./mysqlsniff.py -p $(pgrep -n node) -a __write -f 'CALL getproduct(97)' -S
```

If you visit the Products page again, you'll see that we get the desired query printed to the console, including the Node.js call stack from which this call was invoked! It's an asynchronous stack, so it doesn't show the original application code (only the MySQL module), but this can still occasionally be useful. And in any case, this is the foundation you can use to trace database queries on the application level without any code changes or special agents injected.

- - -
