HOST=localhost
PORT=8000
PERFMAPAGENT=~/src/perf-map-agent
FLAMEGRAPH=~/src/FlameGraph
FGOUT=/tmp/fg.svg
JAVAOPTS=-XX:+PreserveFramePointer

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

clean:
	rm -rf out
