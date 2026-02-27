//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit
import XCTest

final class ProtoParsingTest: XCTestCase {
    func testProtoParsingInvalid() throws {
        XCTAssertThrowsError(try SSKProtoEnvelope(serializedData: Data()))
        XCTAssertThrowsError(try SSKProtoEnvelope(serializedData: Data([1, 2, 3])))
    }
}
