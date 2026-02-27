//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import XCTest

class ZonaRosaProxyTest: XCTestCase {
    func testIsValidProxyLink() throws {
        let validHrefs: [String] = [
            "https://zonarosa.tube/#example.com",
            "sgnl://zonarosa.tube/#example.com",
            "sgnl://zonarosa.tube/extrapath?extra=query#example.com",
            "HTTPS://ZONAROSA.TUBE/#EXAMPLE.COM",
        ]
        for href in validHrefs {
            let url = URL(string: href)!
            XCTAssertTrue(ZonaRosaProxy.isValidProxyLink(url), href)
        }

        let invalidHrefs: [String] = [
            // Wrong protocol
            "http://zonarosa.tube/#example.com",
            // Wrong host
            "https://example.net/#example.com",
            "https://zonarosa.io/#example.com",
            // Extra stuff
            "https://user:pass@zonarosa.tube/#example.com",
            "https://zonarosa.tube:1234/#example.com",
            // Invalid or missing hash
            "https://zonarosa.tube",
            "https://zonarosa.tube/example.com",
            "https://zonarosa.tube/#",
            "https://zonarosa.tube/#example",
            "https://zonarosa.tube/#example.com.",
            "https://zonarosa.tube/#example.com/",
            "https://zonarosa.tube/#\(String(repeating: "x", count: 9999)).example.com",
            "https://zonarosa.tube/#https://example.com",
        ]
        for href in invalidHrefs {
            let url = URL(string: href)!
            XCTAssertFalse(ZonaRosaProxy.isValidProxyLink(url), href)
        }
    }
}
