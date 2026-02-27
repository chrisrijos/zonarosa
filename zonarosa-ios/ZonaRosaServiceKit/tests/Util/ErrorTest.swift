//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest

@testable import ZonaRosaServiceKit

class ErrorTest: XCTestCase {

    func testShortDescription() {
        let error = CocoaError(.fileReadNoSuchFile, userInfo: [NSUnderlyingErrorKey: POSIXError(.ENOENT)])
        XCTAssertEqual(error.shortDescription, "NSCocoaErrorDomain/260, NSPOSIXErrorDomain/2")
    }

}
