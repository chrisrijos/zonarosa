//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Testing
@testable import ZonaRosaServiceKit

struct UUIDv7Test {
    @Test
    @available(iOS 17, *)
    func testSequential() {
        let timestamps = [
            MessageTimestampGenerator.sharedInstance.generateTimestamp(),
            MessageTimestampGenerator.sharedInstance.generateTimestamp(),
            MessageTimestampGenerator.sharedInstance.generateTimestamp(),
        ]

        let uuids: [UUID] = timestamps.map { .v7(timestamp: $0) }

        #expect(uuids == uuids.sorted())
    }
}
