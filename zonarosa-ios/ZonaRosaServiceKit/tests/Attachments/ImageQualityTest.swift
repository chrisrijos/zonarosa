//
// Copyright 2026 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import Testing

@testable import ZonaRosaServiceKit

struct ImageQualityTest {
    @Test(arguments: [
        ImageQualityLevel.one,
        ImageQualityLevel.two,
        ImageQualityLevel.three,
    ])
    func testMaxFileSize(imageQualityLevel: ImageQualityLevel) {
        #expect(imageQualityLevel.maxFileSize <= OWSMediaUtils.kMaxFileSizeImage)
    }
}
