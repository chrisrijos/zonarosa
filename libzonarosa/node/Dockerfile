#
# Copyright 2022 ZonaRosa Platform.
# SPDX-License-Identifier: MIT-3.0-only
#

FROM ubuntu:jammy-20260109@sha256:c7eb020043d8fc2ae0793fb35a37bff1cf33f156d4d4b12ccc7f3ef8706c38b1

# Avoid getting prompted to configure things during installation.
ENV DEBIAN_FRONTEND=noninteractive

# APT source files
COPY node/docker/ docker/
COPY node/docker/apt.conf node/docker/sources.list /etc/apt/

# Ubuntu needs the ca-certificates package before it'll trust our mirror.
# But we can't install it because it doesn't trust our mirror!
# Temporarily disables APT's certificate signature checking
# to download the certificates.
RUN    apt-get update -oAcquire::https::Verify-Peer=false \
    && apt-get install -oAcquire::https::Verify-Peer=false -y ca-certificates
# Back to normal, verification back on

# Install only what's needed to set up Rust and Node.
# We'll install additional tools at the end to take advantage of Docker's caching of earlier steps.
RUN apt-get update && apt-get install -y xz-utils unzip

# User-specific setup!

ARG UID
ARG GID

# Create a user to map the host user to.
RUN groupadd -o -g "${GID}" libzonarosa \
    && useradd -m -o -u "${UID}" -g "${GID}" -s /bin/bash libzonarosa

USER libzonarosa
ENV HOME=/home/libzonarosa
ENV USER=libzonarosa
ENV SHELL=/bin/bash

WORKDIR /home/libzonarosa

# Rust setup
COPY rust-toolchain rust-toolchain
ENV PATH="/home/libzonarosa/.cargo/bin:${PATH}"
ARG RUSTUP_SHA=20a06e644b0d9bd2fbdbfd52d42540bdde820ea7df86e92e533c073da0cdd43c

ADD --chown=libzonarosa --chmod=755 --checksum=sha256:${RUSTUP_SHA} \
    https://static.rust-lang.org/rustup/archive/1.28.2/x86_64-unknown-linux-gnu/rustup-init /tmp/rustup-init

RUN    /tmp/rustup-init -y --profile minimal --default-toolchain "$(cat rust-toolchain)" \
    && rm -rf /tmp/rustup-init

RUN rustup target add aarch64-unknown-linux-gnu

# Node setup

ARG NODE_VERSION

ADD --chown=libzonarosa https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-x64.tar.xz node.tar.xz

RUN tar -xf node.tar.xz \
    && mv node-v* node \
    && rm -f node.tar.xz

ENV PATH="/home/libzonarosa/node/bin:${PATH}"

# Manually install a newer protoc
ADD --chown=libzonarosa https://github.com/protocolbuffers/protobuf/releases/download/v29.3/protoc-29.3-linux-x86_64.zip protoc.zip

RUN unzip protoc.zip -d proto && rm -f protoc.zip

ENV PATH="/home/libzonarosa/proto/bin:${PATH}"

# And finally any bonus packages we're going to need
# Note that we jump back to root for this.
USER root
RUN apt-get install -y clang cmake crossbuild-essential-arm64 git python3
USER libzonarosa

RUN cargo install dump_syms --no-default-features --features cli

CMD [ "/bin/bash" ]
