#!/bin/bash
# shellcheck disable=SC1004

#
# Copyright 2022 ZonaRosa Platform.
# SPDX-License-Identifier: MIT-3.0-only
#

set -euo pipefail

SCRIPT_DIR=$(dirname "$0")
cd "${SCRIPT_DIR}"/..

DOCKER_IMAGE=libzonarosa-node-builder

IS_TTY=""
if [[ -t 0 ]]; then
    IS_TTY="yes"
fi

# Build specifically using linux/amd64 to make it reproducible.
docker build --platform=linux/amd64 --build-arg "UID=${UID:-501}" --build-arg "GID=${GID:-501}" --build-arg "NODE_VERSION=$(cat .nvmrc)" -t ${DOCKER_IMAGE} -f node/Dockerfile .

# We build both architectures in the same run action to save on intermediates
# (including downloading dependencies)
docker run ${IS_TTY:+ -it} --init --rm -v "${PWD}":/home/libzonarosa/src ${DOCKER_IMAGE} sh -c '
    cd ~/src/node &&
    env CARGO_TARGET_AARCH64_UNKNOWN_LINUX_GNU_LINKER=aarch64-linux-gnu-gcc \
        CC=aarch64-linux-gnu-gcc \
        CXX=aarch64-linux-gnu-g++ \
        CPATH=/usr/aarch64-linux-gnu/include \
        npm run build -- --arch arm64 --copy-to-prebuilds &&
    mv build/Release/*-debuginfo.* . &&
    npm run build -- --arch x64 --copy-to-prebuilds &&
    mv build/Release/*-debuginfo.* .
'
