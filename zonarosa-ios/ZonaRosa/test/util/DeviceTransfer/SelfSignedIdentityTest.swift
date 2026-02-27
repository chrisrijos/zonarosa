//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest

@testable import ZonaRosa

class SelfSignedIdentityTest: XCTestCase {
    func testCreate() throws {
        let identity = try SelfSignedIdentity.create(name: "DeviceTransfer", validForDays: 1)

        var certificate: SecCertificate?
        SecIdentityCopyCertificate(identity, &certificate)
        let summary = certificate.flatMap { SecCertificateCopySubjectSummary($0) } as String?
        XCTAssertEqual(summary, "DeviceTransfer")
    }
}
