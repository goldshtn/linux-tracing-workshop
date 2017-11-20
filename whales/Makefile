SOURCE := $(shell readlink -f ..)

define parprimes
	sudo docker run --name primes -d --rm -v $(SOURCE):/src gcc:4.9 \
		sh -c "gcc -g -std=c11 -fno-omit-frame-pointer -fopenmp \
		/src/parprimes.c -o /parprimes && \
		yes | /parprimes 4 100000000"
endef

define continue
	@read -p "Press any key to continue..." -n 1 -r
endef

use:
	sudo docker run --name stress -d --rm progrium/stress --cpu 2
	sudo timeout 5 docker stats || true
	$(call continue)
	systemd-cgtop -n 5 -d 1
	$(call continue)
	sudo docker exec -it stress top -n 5 -d 1
	sudo docker kill stress

profile:
	$(call parprimes)
	sudo perf record -a -g -F 97 -- sleep 10
	sudo perf report --stdio
	sudo docker kill primes

profilegood:
	$(call parprimes)
	sudo /usr/share/bcc/tools/profile -F 97 10
	sudo docker kill primes

io:
	sudo docker run --name stress -d --rm \
		--device-write-iops /dev/xvda:10 progrium/stress --hdd 1
	sudo /usr/share/bcc/tools/biolatency 10 1
	$(call continue)
	sudo timeout 10 /usr/share/bcc/tools/fileslower 10 || true
	sudo docker kill stress
