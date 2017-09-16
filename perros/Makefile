HOST=localhost
PORT=8000
PERFMAPAGENT=~/src/perf-map-agent
FLAMEGRAPH=~/src/FlameGraph
FGOUT=/tmp/fg.svg
JAVAOPTS=-XX:+PreserveFramePointer
APROFILER=~/src/async-profiler

build:
	mkdir -p out
	javac *.java -d out

run:
	@java $(JAVAOPTS) -cp out perros/App $(PORT)

authbench:
	ab -p post-auth.json -c 10 -n 10000 http://$(HOST):$(PORT)/auth

authrecord:
	$(PERFMAPAGENT)/bin/create-java-perf-map.sh `pidof java`
	sudo perf record -a -F 97 -g -- sleep 10
	sudo perf script -f | $(FLAMEGRAPH)/stackcollapse-perf.pl | \
		$(FLAMEGRAPH)/flamegraph.pl --colors=java > $(FGOUT)
	curl --upload-file $(FGOUT) https://transfer.sh

registerbenchgood:
	ab -p post-register.json -n 100000 http://$(HOST):$(PORT)/register

registerbenchbad:
	ab -p post-register-bad.json -n 1000 http://$(HOST):$(PORT)/register

registerrecord:
	sudo bash -c 'echo 1 > /proc/sys/kernel/perf_event_paranoid'
	sudo bash -c 'echo 0 > /proc/sys/kernel/kptr_restrict'
	$(APROFILER)/profiler.sh -d 10 -o summary,flat `pidof java`
	$(APROFILER)/profiler.sh -d 10 -o collapsed -f /tmp/java.stacks `pidof java`
	$(FLAMEGRAPH)/flamegraph.pl --colors=java < /tmp/java.stacks > $(FGOUT)
	curl --upload-file $(FGOUT) https://transfer.sh

clean:
	rm -rf out
