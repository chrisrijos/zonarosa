//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

fn main() {
    // Set environment variables for bridge_fn to produce correctly-named symbols for FFI and JNI.
    println!("cargo:rustc-env=LIBZONAROSA_BRIDGE_FN_PREFIX_FFI=zonarosa_");
}
