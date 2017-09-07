FROM goldshtn/dotnet-runtime:latest
LABEL maintainer="Sasha Goldshtein <goldshtn@gmail.com>"

RUN apt update -y && apt install -y lldb-3.6 && rm -rf /var/lib/apt/lists/*

VOLUME app

ENTRYPOINT ["lldb-3.6", "-c", "/app/core", "/app/Gatos"]
