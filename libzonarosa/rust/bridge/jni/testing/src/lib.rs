//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

// Import bridged functions. Without this, the compiler and/or linker are too
// smart and don't include the symbols in the library.
#[expect(unused_imports)]
use libzonarosa_bridge_testing::*;
#[expect(unused_imports)]
use libzonarosa_jni_impl::*;
