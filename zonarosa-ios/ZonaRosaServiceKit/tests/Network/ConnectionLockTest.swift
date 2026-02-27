//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import Testing

@testable import ZonaRosaServiceKit

struct ConnectionLockTest {
    @Test
    func testEachPriority() async throws {
        let filePath = OWSFileSystem.temporaryFilePath(fileExtension: "lock", isAvailableWhileDeviceLocked: false)
        for priority in 1...3 {
            let connectionLock = ConnectionLock(filePath: filePath, priority: priority, of: 3)
            defer { connectionLock.close() }
            // Ensure that we can lock, unlock, and then lock again.
            for _ in 1...2 {
                let heldLock = try await connectionLock.lock(onInterrupt: (.global(), {}))
                connectionLock.unlock(heldLock)
            }
        }
    }
}
