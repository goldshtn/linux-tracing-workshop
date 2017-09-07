FROM ubuntu:14.04
LABEL maintainer="Sasha Goldshtein <goldshtn@gmail.com>"

RUN apt update -y && apt install -y lldb-3.6 libunwind8

VOLUME app

ENTRYPOINT ["lldb-3.6", "-c", "/app/core", "/app/Gatos"]
