FROM ubuntu:14.04
LABEL maintainer="Sasha Goldshtein <goldshtn@gmail.com>"

RUN apt update && apt install -y \
        ca-certificates libc6 libcurl3 libgcc1 libgssapi-krb5-2 libicu52 \
        liblttng-ust0 libssl1.0.0 libstdc++6 libunwind8 libuuid1 zlib1g \
        && rm -rf /var/lib/apt/lists/*

VOLUME app

ENTRYPOINT ["/app/Gatos"]
