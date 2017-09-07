SRCROOT = $(CURDIR)
LABROOT = /home/fedora/tracing-workshop
FLAMEGRAPH = $(LABROOT)/FlameGraph
PERFTOOLS = $(LABROOT)/perf-tools
CORECLR = $(SRCROOT)/out/libcoreclr.so
PORT = 8888

dockersvc:
	sudo service docker start

build:
	dotnet publish -c Release -o $(SRCROOT)/out --self-contained -r linux-x64

run:
	ASPNETCORE_URLS="http://*:$(PORT)" COMPlus_PerfMapEnabled=1 \
			COMPlus_EnableEventLog=1 \
			$(SRCROOT)/out/Gatos

dockerrun:
	sudo docker rm -f gatos || true
	sudo docker build -t goldshtn/dotnet-runtime:latest - < $(SRCROOT)/Dockerfile.runtime
	sudo docker run -d -p $(PORT):$(PORT) -v $(SRCROOT)/out/:/app --name gatos  \
		-e 'ASPNETCORE_URLS=http://*:8888' goldshtn/dotnet-runtime:latest

authbench:
	ab -n 10000 -c 10 -T 'application/json' -p ./auth.json localhost:$(PORT)/api/auth

authrecord:
	sudo perf record -a -g -F 97 -- sleep 10
	sudo chown fedora perf.data
	perf script | $(FLAMEGRAPH)/stackcollapse-perf.pl \
		| $(FLAMEGRAPH)/flamegraph.pl > /tmp/dotnet.svg
	curl --upload-file /tmp/dotnet.svg https://transfer.sh 2>/dev/null

putstatsbench:
	ab -n 100000 -T 'application/json' -u ./stats.json localhost:$(PORT)/api/stats

putstatsrecord:
	sudo $(PERFTOOLS)/bin/iolatency 1 10
	sudo perf record -a -e block:block_rq_issue -- sleep 10
	sudo chown fedora perf.data
	perf report -n --sort comm,bytes --stdio

getstatsbench:
	ab -n 10000 localhost:$(PORT)/api/stats

getstatsrecord:
	sudo $(PERFTOOLS)/bin/uprobe \
		"p:$(CORECLR):_ZN3SVR6GCHeap24GarbageCollectGenerationEj9gc_reason"

catssingle:
	curl -v -H 'Content-Type: application/json' \
		--data '{"maxweight":42, "maxage":3}' localhost:$(PORT)/api/cat

catsrecord:
	sudo $(PERFTOOLS)/bin/kprobe -p `pidof Gatos` 'p:tcp_sendmsg size=%dx'

gcrecord:
	sudo lttng create gc-trace --output /tmp
	sudo lttng add-context --userspace --type vpid
	sudo lttng add-context --userspace --type vtid
	sudo lttng add-context --userspace --type procname
	sudo lttng enable-event --userspace --tracepoint DotNETRuntime:GCStart*
	sudo lttng enable-event --userspace --tracepoint DotNETRuntime:GCEnd*
	sudo lttng enable-event --userspace --tracepoint DotNETRuntime:GCHeapStats*
	sudo lttng enable-event --userspace --tracepoint DotNETRuntime:GCAllocationTick*
	sudo lttng enable-event --userspace --tracepoint DotNETRuntime:GCTriggered
	sudo lttng start
	sleep 10
	sudo lttng stop
	sudo lttng destroy

gcview:
	sudo babeltrace /tmp

gcallocstats:
	sudo babeltrace /tmp | grep GCAllocationTick | grep 'TypeName = "[^"]*"' -o \
		| sort | uniq -c | sort -n

catsbench:
	ab -n 1000 -c 10 -T 'application/json' -p cat.json localhost:$(PORT)/api/cat

catsrecord2:
	pidstat -p `pidof Gatos` 1 10
	sudo /usr/share/bcc/tools/offcputime -p `pidof Gatos` -f 10 > /tmp/offcpu.stacks
	$(FLAMEGRAPH)/flamegraph.pl < /tmp/offcpu.stacks > /tmp/offcpu.svg
	curl --upload-file /tmp/offcpu.svg https://transfer.sh 2>/dev/null

alloccount:
	sudo /usr/share/bcc/tools/funccount -d 10 "$(CORECLR):*New*"

allocstacks:
	sudo /usr/share/bcc/tools/stackcount -D 10 -f "$(CORECLR):*New*" > /tmp/alloc.stacks
	$(FLAMEGRAPH)/flamegraph.pl < /tmp/alloc.stacks > /tmp/alloc.svg
	curl --upload-file /tmp/alloc.svg https://transfer.sh 2>/dev/null

update:
	echo core | sudo tee /proc/sys/kernel/core_pattern
	curl -v -H 'Content-Type: application/json' --data "" localhost:$(PORT)/api/cat/update
	@echo "Nothing bad seems to have happened. But now let's run some more requests:"
	sleep 10
	make getstatsbench

updatelogs:
	sudo docker logs gatos

updateanalyze:
	sudo docker cp gatos:/core.1 $(SRCROOT)/out/core
	sudo docker build -t analyzer - < $(SRCROOT)/Dockerfile.analysis
	sudo docker run --rm -it -v $(SRCROOT)/out:/app analyzer \
		-o "plugin load /app/libsosplugin.so"
	sudo docker rm gatos
	sudo docker image prune -f
