//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import XCTest

@testable import LibZonaRosaClient
@testable import ZonaRosaFfi

class IoTests: TestCaseBase {
    // These testing endpoints aren't generated in device builds, to save on code size.
    #if !os(iOS) || targetEnvironment(simulator)
    func testReadIntoEmptyBuffer() throws {
        let input = Data("ABCDEFGHIJKLMNOPQRSTUVWXYZ".utf8)
        let inputStream = ZonaRosaInputStreamAdapter(input)
        let output = try withInputStream(inputStream) { input in
            try invokeFnReturningData { output in
                ZonaRosaFfi.zonarosa_testing_input_stream_read_into_zero_length_slice(output, input)
            }
        }
        XCTAssertEqual(input, output)
    }
    #endif
}
