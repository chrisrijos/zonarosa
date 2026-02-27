//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest
@testable import ZonaRosaServiceKit

class TSGroupThreadTest: XCTestCase {
    func testHasSafetyNumbers() throws {
        let groupThread = TSGroupThread.forUnitTest()
        XCTAssertFalse(groupThread.hasSafetyNumbers())
    }
}
