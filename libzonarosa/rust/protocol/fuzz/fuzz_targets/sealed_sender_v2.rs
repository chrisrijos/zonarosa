//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

#![no_main]

use libfuzzer_sys::fuzz_target;
use libzonarosa_protocol::*;

fuzz_target!(|data: &[u8]| {
    let _: Result<_, _> = SealedSenderV2SentMessage::parse(data);
});
