//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

fn main() {
    // Set environment variables for bridge_fn to produce correctly-named symbols for FFI and JNI.
    println!("cargo:rustc-env=LIBZONAROSA_BRIDGE_FN_PREFIX_FFI=zonarosa_");
    // This naming convention comes from JNI:
    // https://docs.oracle.com/en/java/javase/20/docs/specs/jni/design.html#resolving-native-method-names
    println!(
        "cargo:rustc-env=LIBZONAROSA_BRIDGE_FN_PREFIX_JNI=Java_org_zonarosa_libzonarosa_internal_NativeTesting_"
    );
}
