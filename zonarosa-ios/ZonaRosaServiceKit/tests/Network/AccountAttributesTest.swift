//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest

@testable import ZonaRosaServiceKit

final class AccountAttributesTest: XCTestCase {
    func testCapabilitiesRequestParameters() {
        let capabilities = AccountAttributes.Capabilities(hasSVRBackups: true)
        let requestParameters = capabilities.requestParameters
        // All we care about is that the prior line didn't crash.
        XCTAssertGreaterThanOrEqual(requestParameters.count, 0)
    }
}
