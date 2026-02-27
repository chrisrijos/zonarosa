//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import Testing

@testable import ZonaRosaServiceKit

struct TimeIntervalTest {
    @Test(arguments: [
        (.infinity, UInt64(Int64.max)),
        (.nan, 0),
        (-1.5, 0),
        (0.5, 500_000_000),
    ] as [(inputValue: TimeInterval, expectedValue: UInt64)])
    func testClampedNanoseconds(testCase: (inputValue: TimeInterval, expectedValue: UInt64)) {
        #expect(testCase.inputValue.clampedNanoseconds == testCase.expectedValue)
    }
}
