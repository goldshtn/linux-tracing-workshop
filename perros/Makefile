ALLROOT=~/tracing-workshop
LABSROOT=$(ALLROOT)/labs
HOST=localhost
PORT=8000
PERFMAPAGENT=$(ALLROOT)/perf-map-agent
FLAMEGRAPH=$(ALLROOT)/FlameGraph
FGOUT=/tmp/fg.svg
JAVACFLAGS=-Xlint:unchecked
JAVAOPTS=-XX:+PreserveFramePointer
APROFILER=$(ALLROOT)/async-profiler
BCC=/usr/share/bcc/tools
MYSQLCONNJAR=$(LABSROOT)/buggy/mysql-connector-java-5.1.40-bin.jar
JVM=$(shell find /usr/lib/jvm -name libjvm.so -print -quit)

dockersvc:
	sudo service docker start

build:
	mkdir -p out
	cp $(MYSQLCONNJAR) out/
	javac $(JAVACFLAGS) *.java -d out

run:
	@java $(JAVAOPTS) \
		-cp "out:out/`basename $(MYSQLCONNJAR)`" perros/App $(PORT)

runext:
	@java $(JAVAOPTS) -XX:+ExtendedDTraceProbes \
		-cp "out:out/`basename $(MYSQLCONNJAR)`" perros/App $(PORT)

dockerrun:
	sudo docker run -p $(PORT):$(PORT) --rm -it -v $(CURDIR)/out:/app \
		--name perros openjdk:8 java $(JAVAOPTS) -cp /app perros/App $(PORT)

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
	./perf-std.user.sh
	$(APROFILER)/profiler.sh -d 10 -o summary,flat `pidof java`
	$(APROFILER)/profiler.sh -d 10 -o collapsed -f /tmp/java.stacks `pidof java`
	$(FLAMEGRAPH)/flamegraph.pl --colors=java < /tmp/java.stacks > $(FGOUT)
	curl --upload-file $(FGOUT) https://transfer.sh

adminrepro:
	curl "$(HOST):$(PORT)/admin?username=david" -v

adminsnoop1:
	sudo $(BCC)/opensnoop -p `pidof java` -x

adminsnoop2:
	$(PERFMAPAGENT)/bin/create-java-perf-map.sh `pidof java`
	sudo $(BCC)/trace -p `pidof java` \
		-U 'r::SyS_open (retval==-2) "failed open"'

adminfix:
	echo 'approve: david' | sudo tee /etc/perros.conf

usersbench:
	$(LABSROOT)/mysql-db.sh
	./usersbench.sh "$(HOST):$(PORT)"

usersrecord1:
	sudo $(BCC)/dbslower mysql -m 1000

usersrecord2:
	sudo $(BCC)/trace 'u::query__exec__start "%s", arg1' -p `pidof mysqld`

usersrecord3:
	$(PERFMAPAGENT)/bin/create-java-perf-map.sh `pidof java`
	sudo $(LABSROOT)/mysqlsniff.py -p `pidof java` -f "call getproduct(97)" -S

statsbench:
	ab -n 10000 "$(HOST):$(PORT)/stats?pages=10000"

statsrecord1:
	./perf-std-user.sh
	$(APROFILER)/profiler.sh -d 10 -m heap -o summary,flat `pidof java`
	$(APROFILER)/profiler.sh -d 10 -m heap -o collapsed \
		-f /tmp/java.stacks `pidof java`
	$(FLAMEGRAPH)/flamegraph.pl --colors=java < /tmp/java.stacks > $(FGOUT)
	curl --upload-file $(FGOUT) https://transfer.sh

statsrecord2:
	$(PERFMAPAGENT)/bin/create-java-perf-map.sh `pidof java` dottedclass
	sudo $(BCC)/funccount "$(JVM):*AllocTracer*" -p `pidof java` -d 5
	sudo $(BCC)/stackcount "$(JVM):*AllocTracer*" -p `pidof java` -f \
		-D 10 > /tmp/java.stacks
	$(FLAMEGRAPH)/flamegraph.pl --colors=java < /tmp/java.stacks > $(FGOUT)
	curl --upload-file $(FGOUT) https://transfer.sh

statsrecord3:
	@echo Hit Ctrl+C after a few seconds to see the results from javaobjnew.
	sudo $(BCC)/javaobjnew `pidof java` -C 10

statsrecord4:
	$(PERFMAPAGENT)/bin/create-java-perf-map.sh `pidof java` dottedclass
	sudo $(BCC)/stackcount "u:$(JVM):object__alloc" -p `pidof java` -f \
		-D 10 > /tmp/java.stacks
	$(FLAMEGRAPH)/flamegraph.pl --colors=java < /tmp/java.stacks > $(FGOUT)
	curl --upload-file $(FGOUT) https://transfer.sh

clean:
	rm -rf out
