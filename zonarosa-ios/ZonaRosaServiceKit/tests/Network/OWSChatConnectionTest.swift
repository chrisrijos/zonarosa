//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import Testing

@testable import ZonaRosaServiceKit

struct OWSChatConnectionTest {
    @Test
    func testReconnectAfterFailureDelay() {
        // The reconnectAfterFailure method makes an assumption about the behavior
        // of retryIntervalForExponentialBackoff. This test exists to fail if that
        // assumption is ever violated. If this test fails, you MUST look at
        // reconnectAfterFailure.
        let aWhileDelay = 2 * OWSChatConnection.socketReconnectDelay
        for _ in 0...1024 {
            let retryInterval = OWSOperation.retryIntervalForExponentialBackoff(
                failureCount: Int8.max,
                maxAverageBackoff: OWSChatConnection.socketReconnectDelay,
            )
            #expect(retryInterval < aWhileDelay)
        }
    }
}
