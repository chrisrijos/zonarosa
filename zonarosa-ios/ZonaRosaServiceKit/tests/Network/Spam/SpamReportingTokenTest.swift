//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import XCTest

final class SpamReportingTokenTest: XCTestCase {
    func testInit() {
        XCTAssertNil(SpamReportingToken(data: .init()))
        XCTAssertNotNil(SpamReportingToken(data: .init([1, 2, 3])))
    }

    func testBase64EncodedString() {
        XCTAssertEqual(
            SpamReportingToken(data: .init([1, 2, 3, 4]))?.base64EncodedString(),
            "AQIDBA==",
        )
    }
}
