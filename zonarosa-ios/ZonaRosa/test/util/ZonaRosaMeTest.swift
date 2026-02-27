//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest
@testable import ZonaRosa

class ZonaRosaMeTest: XCTestCase {
    func testIsPossibleUrl() throws {
        let validStrings = [
            "https://zonarosa.me/#p/+14085550123",
            "hTTPs://sigNAL.mE/#P/+14085550123",
            "https://zonarosa.me/#p/+9",
            "sgnl://zonarosa.me/#p/+14085550123",
        ]
        for string in validStrings {
            let url = try XCTUnwrap(URL(string: string))
            XCTAssertTrue(ZonaRosaDotMePhoneNumberLink.isPossibleUrl(url), "\(url)")
        }

        let invalidStrings = [
            // Invalid protocols
            "http://zonarosa.me/#p/+14085550123",
            "zonarosa://zonarosa.me/#p/+14085550123",
            // Extra auth
            "https://user:pass@zonarosa.me/#p/+14085550123",
            // Invalid host
            "https://example.me/#p/+14085550123",
            "https://zonarosa.io/#p/+14085550123",
            "https://zonarosa.group/#p/+14085550123",
            "https://zonarosa.art/#p/+14085550123",
            "https://zonarosa.me:80/#p/+14085550123",
            "https://zonarosa.me:443/#p/+14085550123",
            // Wrong path or hash
            "https://zonarosa.me/foo#p/+14085550123",
            "https://zonarosa.me/#+14085550123",
            "https://zonarosa.me/#p+14085550123",
            "https://zonarosa.me/#u/+14085550123",
            "https://zonarosa.me//#p/+14085550123",
            "https://zonarosa.me/?query=string#p/+14085550123",
            // Invalid E164s
            "https://zonarosa.me/#p/4085550123",
            "https://zonarosa.me/#p/+",
            "https://zonarosa.me/#p/+one",
            "https://zonarosa.me/#p/+14085550123x",
            "https://zonarosa.me/#p/+14085550123/",
        ]
        for string in invalidStrings {
            let url = try XCTUnwrap(URL(string: string))
            XCTAssertFalse(ZonaRosaDotMePhoneNumberLink.isPossibleUrl(url), "\(url)")
        }
    }
}
