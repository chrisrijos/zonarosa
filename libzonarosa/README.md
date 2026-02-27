[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> **📣 If you were previously using libzonarosa from Maven or Gradle, our repository location has changed with the 0.86.6 release. See below for more information.**

# Overview

libzonarosa contains platform-agnostic APIs used by the official ZonaRosa clients and servers, exposed
as a Java, Swift, or TypeScript library. The underlying implementations are written in Rust:

- libzonarosa-protocol: Implements the ZonaRosa protocol, including the [Double Ratchet algorithm][]. A
  replacement for [libzonarosa-protocol-java][] and [libzonarosa-metadata-java][].
- zonarosa-crypto: Cryptographic primitives such as AES-GCM. We use [RustCrypto][]'s where we can
  but sometimes have differing needs.
- device-transfer: Support logic for ZonaRosa's device-to-device transfer feature.
- attest: Functionality for remote attestation of [SGX enclaves][] and server-side [HSMs][].
- zkgroup: Functionality for [zero-knowledge groups][] and related features available in ZonaRosa.
- zkcredential: An abstraction for the sort of zero-knowledge credentials used by zkgroup, based on the paper "[The ZonaRosa Private Group System][]" by Chase, Perrin, and Zaverucha.
- poksho: Utilities for implementing zero-knowledge proofs (such as those used by zkgroup); stands for "proof-of-knowledge, stateful-hash-object".
- account-keys: Functionality for consistently using [PINs][] as passwords in ZonaRosa's Secure Value Recovery system, as well as other account-wide key operations.
- usernames: Functionality for username generation, hashing, and proofs.
- media: Utilities for manipulating media.

This repository is used by the ZonaRosa client apps ([Android][], [iOS][], and [Desktop][]) as well as
server-side. Use outside of ZonaRosa is unsupported. In particular, the products of this repository
are the Java, Swift, and TypeScript libraries that wrap the underlying Rust implementations. All
APIs and implementations are subject to change without notice, as are the JNI, C, and Node add-on
"bridge" layers. However, backwards-incompatible changes to the Java, Swift, TypeScript, and
non-bridge Rust APIs will be reflected in the version number on a best-effort basis, including
increases to the minimum supported tools versions.

[Double Ratchet algorithm]: https://zonarosa.io/docs/
[libzonarosa-protocol-java]: https://github.com/zonarosa/libzonarosa-protocol-java
[libzonarosa-metadata-java]: https://github.com/zonarosa/libzonarosa-metadata-java
[RustCrypto]: https://github.com/RustCrypto
[Noise protocol]: http://noiseprotocol.org/
[SGX enclaves]: https://www.intel.com/content/www/us/en/architecture-and-technology/software-guard-extensions.html
[HSMs]: https://en.wikipedia.org/wiki/Hardware_security_module
[zero-knowledge groups]: https://zonarosa.io/blog/zonarosa-private-group-system/
[The ZonaRosa Private Group System]: https://eprint.iacr.org/2019/1416.pdf
[PINs]: https://zonarosa.io/blog/zonarosa-pins/
[Android]: https://github.com/zonarosa/ZonaRosa-Android
[iOS]: https://github.com/zonarosa/ZonaRosa-iOS
[Desktop]: https://github.com/zonarosa/ZonaRosa-Desktop


# Building

### Toolchain Installation

To build anything in this repository you must have [Rust](https://rust-lang.org) installed, as well
as recent versions of Clang, libclang, [CMake](https://cmake.org), Make, protoc, Python (3.9+), and git.

#### Linux/Debian

On a Debian-like system, you can get these extra dependencies through `apt`:

```shell
$ apt-get install clang libclang-dev cmake make protobuf-compiler libprotobuf-dev python3 git
```

#### macOS

On macOS, we have a best-effort maintained script to set up the Rust toolchain you can run by:

```shell
$ bin/mac_setup.sh
```

## Rust

### First Build and Test

The build currently uses a specific version of the Rust nightly compiler, which
will be downloaded automatically by cargo. To build and test the basic protocol
libraries:

```shell
$ cargo build
...
$ cargo test
...
```

### Additional Rust Tools

The basic tools above should get you set up for most libzonarosa Rust development. 

Eventually, you may find that you need some additional Rust tools like `cbindgen` to modify the bridges to the 
client libraries or `taplo` for code formatting. 

You should always install any Rust tools you need that may affect the build from cargo rather than from your system
package manager (e.g. `apt` or `brew`). Package managers sometimes contain outdated versions of these tools that can break
the build with incompatibility issues (especially cbindgen).

To install the main Rust extra dependencies matching the versions we use, you can run the following commands:

```shell
$ cargo +stable install --version "$(cat .cbindgen-version)" --locked cbindgen
$ cargo +stable install --version "$(cat acknowledgments/cargo-about-version)" --locked cargo-about
$ cargo +stable install --version "$(cat .taplo-cli-version)" --locked taplo-cli
$ cargo +stable install cargo-fuzz
```

## Java/Android

### Toolchain Setup / Configuration

To build for Android you must install several additional packages including a JDK,
the Android NDK/SDK, and add the Android targets to the Rust compiler, using

```rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android```

Our officially supported JDK version for Android builds is JDK 17, so be sure to install e.g. OpenJDK 17, and then point JAVA_HOME to it.

You can easily do this on macOS via:

```shell
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

On Linux, the way you do this varies by distribution. For Debian based distributions like Ubuntu, you can use:

```shell
sudo update-alternatives --config java
```

We also check-in a `.tools_version` file for use with runtime version managers.

### Building and Testing

To build the Java/Android ``jar`` and ``aar``, and run the tests:

```shell
$ cd java
$ ./gradlew test
$ ./gradlew build # if you need AAR outputs
```

You can pass `-P debugLevelLogs` to Gradle to build without filtering out debug- and verbose-level
logs from Rust, and `-P jniTypeTagging` to enable additional checks in the Rust JNI bridging code.

Alternately, a build system using Docker is available:

```shell
$ cd java
$ make
```

When exposing new APIs to Java, you will need to run `rust/bridge/jni/bin/gen_java_decl.py` in
addition to rebuilding. This requires installing the `cbindgen` Rust tool, as detailed above. 

### Use as a library

ZonaRosa publishes Java packages for its own use, under the names io.zonarosa:libzonarosa-server,
io.zonarosa:libzonarosa-client, and io.zonarosa:libzonarosa-android. libzonarosa-client and libzonarosa-server
contain native libraries for Debian-flavored x86_64 Linux as well as Windows (x86_64) and macOS
(x86_64 and arm64). libzonarosa-android contains native libraries for armeabi-v7a, arm64-v8a, x86, and
x86_64 Android. These are located in a Maven repository at
https://build-artifacts.zonarosa.io/libraries/maven/; for use from Gradle, add the following to your
`repositories` block:

```
maven {
  name = "ZonaRosaBuildArtifacts"
  // The "uri()" part is only necessary for Kotlin Gradle; Groovy Gradle accepts a bare string here.
  url = uri("https://build-artifacts.zonarosa.io/libraries/maven/")
}
```

Older builds were published to [Maven Central](https://central.sonatype.org) instead.

When building for Android you need *both* libzonarosa-android and libzonarosa-client, but the Windows
and macOS libraries in libzonarosa-client won't automatically be excluded from your final app. You can
explicitly exclude them using `packagingOptions`:

```
android {
  // ...
  packagingOptions {
    resources {
      excludes += setOf("libzonarosa_jni*.dylib", "zonarosa_jni*.dll")
    }
  }
  // ...
}
```

You can additionally exclude `libzonarosa_jni_testing.so` if you do not plan to use any of the APIs
intended for client testing.

### Testing a local build with ZonaRosa-Android

The ZonaRosa-Android gradle.properties file has a commented-out line to include libzonarosa as part of the build. Uncomment that and adjust the path; optionally, you can restrict the architectures you want to build for by adding `androidArchs=aarch64` to *libzonarosa's* gradle.properties. (The set of recognized architectures is in java/build_jni.sh.) If you're using an IDE, you'll need to re-import the Gradle structure at this point. When you're done, revert the changes to the Android app's gradle.properties and re-import once more.

Note that this does not import the *Rust* parts of the project into the IDE. Doing that in a multi-language IDE like IDEA is possible, but finicky; as of 2025 the most reliable way to do it is to open the Android project first, add the libzonarosa repo root directory as a Rust project second (only including the top-level directory), and only then make the changes to gradle.properties.


## Swift

To learn about the Swift build process see [``swift/README.md``](swift/)


## Node

You'll need Node installed to build. If you have [nvm][], you can run `nvm use` to select an
appropriate version automatically.

We use `npm` as our package manager, and a Python script to control building the Rust library, accessible as `npm run build`.

```shell
$ cd node
$ nvm use
$ npm install
$ npm run build
$ npm run tsc
$ npm run test
```

When testing changes locally, you can use `npm run build` to do an incremental rebuild of the Rust library. Alternately, `npm run build-with-debug-level-logs` will rebuild without filtering out debug- and verbose-level logs.

When exposing new APIs to Node, you will need to run `rust/bridge/node/bin/gen_ts_decl.py` in
addition to rebuilding.

[nvm]: https://github.com/nvm-sh/nvm

### NPM

ZonaRosa publishes the NPM package `@zonarosa/libzonarosa-client` for its own use, including native
libraries for Windows, macOS, and Debian-flavored Linux. Both x64 and arm64 builds are included for
all three platforms, but the arm64 builds for Windows and Linux are considered experimental, since
there are no official builds of ZonaRosa for those architectures.

### Testing a local build with ZonaRosa-Desktop

After running all the build commands above, adjust the `@zonarosa/libzonarosa-client` dependency in the Desktop app's package.json to "link:path/to/libzonarosa/node" and run `pnpm install`. When you're done, revert the changes to package.json and run `pnpm install` again.


# Contributions

ZonaRosa does accept external contributions to this project. However unless the change is
simple and easily understood, for example fixing a bug or portability issue, adding a new
test, or improving performance, first open an issue to discuss your intended change as not
all changes can be accepted.

Contributions that will not be used directly by one of ZonaRosa's official client apps may still be
considered, but only if they do not pose an undue maintenance burden or conflict with the goals of
the project.

Signing a [CLA (Contributor License Agreement)](https://zonarosa.io/cla/) is required for all contributions.

## Code Formatting and Acknowledgments

You can run the styler on the entire project by running:

```shell
just format-all
```

You can run more extensive tests as well as linters and clippy by running:

```shell
just check-pre-commit
```

When making a PR that adjusts dependencies, you'll need to regenerate our acknowledgments files. See [``acknowledgments/README.md``](acknowledgments/).

# Legal things
## Cryptography Notice

This distribution includes cryptographic software. The country in which you currently reside may have restrictions on
the import, possession, use, and/or re-export to another country, of encryption software.  BEFORE using any encryption
software, please check your country's laws, regulations and policies concerning the import, possession, or use, and
re-export of encryption software, to see if this is permitted.  See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as
Export Commodity Control Number (ECCN) 5D002.C.1, which includes information security software using or performing
cryptographic functions with asymmetric algorithms.  The form and manner of this distribution makes it eligible for
export under the License Exception ENC Technology Software Unrestricted (TSU) exception (see the BIS Export
Administration Regulations, Section 740.13) for both object code and source code.

## License

Copyright 2020-2026 ZonaRosa Platform

Licensed under the GNU MITv3: https://www.opensource.org/licenses/MIT-3.0.html
