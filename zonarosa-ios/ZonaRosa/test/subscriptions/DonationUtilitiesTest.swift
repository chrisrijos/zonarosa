//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import XCTest

final class DonationUtilitiesTest: XCTestCase {
    func testChooseDefaultCurrency() throws {
        let foundResult = DonationUtilities.chooseDefaultCurrency(
            preferred: ["AUD", "GBP", nil, "USD", "XYZ"],
            supported: ["USD"],
        )
        XCTAssertEqual(foundResult, "USD")

        let noResult = DonationUtilities.chooseDefaultCurrency(
            preferred: ["AUD", "GBP", "USD"],
            supported: ["XYZ"],
        )
        XCTAssertNil(noResult)
    }
}
