//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import Testing

struct IntTest {
    @Test
    func testSafeCast() {
        // This is least safe of the safe casts, though it would require
        // UInt.bitWidth to be larger than UInt64.bitWidth. That's not currently a
        // thing, and it seems unlikely to change in the foreseeable future.
        _ = UInt64(safeCast: UInt.max)
    }
}
