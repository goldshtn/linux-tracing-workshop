ALLROOT=~/tracing-workshop
LABSROOT=$(ALLROOT)/labs
HOST=localhost
PORT=8000
PERFMAPAGENT=$(ALLROOT)/perf-map-agent
FLAMEGRAPH=$(ALLROOT)/FlameGraph
FGOUT=/tmp/fg.svg
JAVAOPTS=-XX:+PreserveFramePointer
APROFILER=$(ALLROOT)/async-profiler
BCC=/usr/share/bcc/tools
MYSQLCONNJAR=$(LABSROOT)/buggy/mysql-connector-java-5.1.40-bin.jar

build:
	mkdir -p out
	cp $(MYSQLCONNJAR) out/
	javac *.java -d out

run:
	@java $(JAVAOPTS) -cp "out:out/`basename $(MYSQLCONNJAR)`" perros/App $(PORT)

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

adminrepro:
	curl 'localhost:8000/admin?username=david' -v

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

clean:
	rm -rf out
