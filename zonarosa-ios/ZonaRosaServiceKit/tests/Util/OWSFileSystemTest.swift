//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import XCTest

class OWSFileSystemTest: XCTestCase {
    func testFreeSpaceInBytes() throws {
        let path = URL(fileURLWithPath: "/tmp")
        let result = try XCTUnwrap(OWSFileSystem.freeSpaceInBytes(forPath: path))
        XCTAssertGreaterThan(result, 1)
    }
}
