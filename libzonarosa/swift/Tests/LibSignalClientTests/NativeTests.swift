//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi
import XCTest

@testable import LibZonaRosaClient

final class NativeTests: XCTestCase {
    // These testing endpoints aren't generated in device builds, to save on code size.
    #if !os(iOS) || targetEnvironment(simulator)
    func testTestingFnsAreAvailable() async throws {
        let output = try invokeFnReturningInteger(fn: ZonaRosaFfi.zonarosa_test_only_fn_returns_123)
        XCTAssertEqual(output, 123)
    }
    #endif
}
