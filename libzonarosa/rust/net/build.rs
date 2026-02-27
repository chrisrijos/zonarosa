//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

fn main() {
    let protos = [
        "src/proto/cds2.proto",
        "src/proto/chat_provisioning.proto",
        "src/proto/chat_websocket.proto",
    ];
    prost_build::Config::new()
        .bytes([
            ".zonarosa.proto.chat_provisioning",
            ".zonarosa.proto.chat_websocket",
        ])
        .compile_protos(&protos, &["src"])
        .expect("Protobufs in src are valid");
    for proto in &protos {
        println!("cargo:rerun-if-changed={proto}");
    }
}
