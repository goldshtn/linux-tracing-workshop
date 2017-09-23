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
CONTAINERNAME=perros
# The allow-perf-event-open.json file is a minor modification of
#   https://github.com/moby/moby/blob/master/profiles/seccomp/default.json
# which whitelists the perf_event_open syscall.
DOCKERFLAGS=-p $(PORT):$(PORT) --rm -it -v $(CURDIR)/out:/app -v /tmp:/tmp \
	--name $(CONTAINERNAME) --security-opt seccomp=allow-perf-event-open.json
DOCKERCMD=perros:latest java $(JAVAOPTS) -cp /app perros/App $(PORT)
CGROUPFS=/sys/fs/cgroup

define flame-graph =
	$(FLAMEGRAPH)/flamegraph.pl --colors=java > $(FGOUT) < /tmp/java.stacks
	curl --upload-file $(FGOUT) https://transfer.sh
endef

define java-run =
	@java $(JAVAOPTS) \
		-cp "out:out/`basename $(MYSQLCONNJAR)`" perros/App $(PORT)
endef

define docker-run =
	sudo docker run $(DOCKERFLAGS) $(DOCKERCMD)
endef

dockerbuild:
	sudo docker build -t perros:latest - < Dockerfile

dockersvc:
	sudo service docker start

build:
	mkdir -p out
	cp $(MYSQLCONNJAR) out/
	javac $(JAVACFLAGS) *.java -d out

buildagent:
	@if test -z "${JAVA_HOME}"; then \
		echo "JAVA_HOME must be set to build the agent."; \
		exit 1; \
	fi;
	g++ -std=c++11 -O2 -fPIC -shared -o libagent.so -ldl -lpthread \
		-I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux agent.cc

run:
	$(java-run)

runext:
	$(eval JAVAOPTS=$(JAVAOPTS) -XX:+ExtendedDTraceProbes)
	$(java-run)

attachagent:
	$(APROFILER)/build/jattach `pidof java` load $(CURDIR)/libagent.so true

dumpagent:
	$(APROFILER)/build/jattach `pidof java` load $(CURDIR)/libagent.so true dump

dockerrun:
	$(docker-run)

dockerrun2:
	$(eval DOCKERFLAGS=$(DOCKERFLAGS) --cpus=0.5)
	$(docker-run)

dockerrun3:		# in the background, run 'make statsbench'
	$(eval DOCKERFLAGS=$(DOCKERFLAGS) --memory=100m)
	$(eval JAVAOPTS=$(JAVAOPTS) -XshowSettings:vm -XX:+PrintGCDetails)
	$(docker-run)

dockerrun4:
	$(eval DOCKERFLAGS=$(DOCKERFLAGS) --memory=100m)
	$(eval JAVAOPTS=$(JAVAOPTS) -XshowSettings:vm -XX:+PrintGCDetails \
		-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap \
		-XX:MaxRAMFraction=1)
	$(docker-run)

authbench:
	ab -p post-auth.json -c 10 -n 10000 http://$(HOST):$(PORT)/auth

authrecord:
	$(PERFMAPAGENT)/bin/create-java-perf-map.sh `pidof java`
	sudo perf record -a -F 97 -g -- sleep 10
	sudo perf script -f | $(FLAMEGRAPH)/stackcollapse-perf.pl > /tmp/java.stacks
	$(flame-graph)

registerbenchgood:
	ab -p post-register.json -n 100000 http://$(HOST):$(PORT)/register

registerbenchbad:
	ab -p post-register-bad.json -n 1000 http://$(HOST):$(PORT)/register

registerrecord:
	./perf-std.user.sh
	$(APROFILER)/profiler.sh -d 10 -o summary,flat `pidof java`
	$(APROFILER)/profiler.sh -d 10 -o collapsed -f /tmp/java.stacks `pidof java`
	$(flame-graph)

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

statsgc:
	sudo $(BCC)/javastat 1 10

statsrecord1:
	./perf-std-user.sh
	$(APROFILER)/profiler.sh -d 10 -m heap -o summary,flat `pidof java`
	$(APROFILER)/profiler.sh -d 10 -m heap -o collapsed \
		-f /tmp/java.stacks `pidof java`
	$(flame-graph)

statsrecord2:
	$(PERFMAPAGENT)/bin/create-java-perf-map.sh `pidof java` dottedclass
	sudo $(BCC)/funccount "$(JVM):*AllocTracer*" -p `pidof java` -d 5
	sudo $(BCC)/stackcount "$(JVM):*AllocTracer*" -p `pidof java` -f \
		-D 10 > /tmp/java.stacks
	$(flame-graph)

statsrecord3:
	@echo Hit Ctrl+C after a few seconds to see the results from javaobjnew.
	sudo $(BCC)/javaobjnew `pidof java` -C 10

statsrecord4:
	$(PERFMAPAGENT)/bin/create-java-perf-map.sh `pidof java` dottedclass
	sudo $(BCC)/stackcount "u:$(JVM):object__alloc" -p `pidof java` -f \
		-D 10 > /tmp/java.stacks
	$(flame-graph)

throttlerecord:
	$(eval CID=`docker inspect $(CONTAINERNAME) --format '{{ .Id }}'`)
	cat $(CGROUPFS)/cpu,cpuacct/docker/$(CID)/cpu.stat
	sudo $(BCC)/runqlat -p `pidof java` -m 5 1
	sudo $(BCC)/cpudist -p `pidof java` -m 5 1
	sudo $(BCC)/cpudist -O -p `pidof java` -m 5 1

throttlerecord2:
	sudo docker exec $(CONTAINERNAME) sh -c 'cd perf-map-agent/out && \
		java -cp attach-main.jar:$$JAVA_HOME/lib/tools.jar \
		net.virtualvoid.perf.AttachOnce `pidof java` dottedclass'
	sudo $(BCC)/offcputime -f -p `pidof java` 5 > /tmp/java.stacks
	$(flame-graph)

profilecontainer:
	# The container inherits these settings from the host
	./perf-std-user.sh
	sudo docker exec $(CONTAINERNAME) sh -c '/async-profiler/profiler.sh \
		-d 10 -o collapsed -f /app/java.stacks `pidof java`'
	mv out/java.stacks /tmp/java.stacks
	$(flame-graph)

clean:
	rm -rf out
