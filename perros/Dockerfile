FROM openjdk:8

RUN apt update && apt install -y cmake g++ \
               && rm -rf /var/lib/apt/lists/*

RUN git clone --depth=1 https://github.com/jvm-profiling-tools/perf-map-agent \
    && cd perf-map-agent && cmake . && make

RUN git clone --depth=1 https://github.com/jvm-profiling-tools/async-profiler \
    && cd async-profiler && make
