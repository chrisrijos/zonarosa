//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import LibZonaRosaClient
import XCTest
@testable import ZonaRosa

class DeviceProvisioningURLTest: XCTestCase {
    func testValid() {
        func isValid(_ provisioningURL: String) -> Bool {
            DeviceProvisioningURL(urlString: provisioningURL) != nil
        }

        XCTAssertFalse(isValid(""))
        XCTAssertFalse(isValid("sgnl://linkdevice?uuid=BQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))
        XCTAssertFalse(isValid("sgnl://linkdevice?pub_key=BQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))
        XCTAssertFalse(isValid("sgnl://linkdevice/uuid=asd&pub_key=BQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))

        XCTAssertTrue(isValid("sgnl://linkdevice?uuid=asd&pub_key=BQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))
    }

    func testPublicKey() throws {
        let url = try XCTUnwrap(DeviceProvisioningURL(urlString: "sgnl://linkdevice?uuid=asd&pub_key=BQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))

        XCTAssertEqual(url.publicKey, try PublicKey(keyData: Data(repeating: 0, count: 32)))
    }

    func testEphemeralDeviceId() throws {
        let url = try XCTUnwrap(DeviceProvisioningURL(urlString: "sgnl://linkdevice?uuid=asd&pub_key=BQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))

        XCTAssertEqual(url.ephemeralDeviceId, "asd")
    }
}
