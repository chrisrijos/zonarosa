//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import Testing

@testable import ZonaRosaServiceKit

struct ImageMetadataTest {
    @Test(arguments: [
        ImageFormat.png,
        ImageFormat.gif,
        ImageFormat.tiff,
        ImageFormat.jpeg,
        ImageFormat.bmp,
        ImageFormat.webp,
        ImageFormat.heic,
        ImageFormat.heif,
    ])
    func testFileExtension(imageFormat: ImageFormat) {
        let expectedFileExtension = { (imageFormat: ImageFormat) -> String in
            // When adding a new case, add a new argument.
            switch imageFormat {
            case .png: "png"
            case .gif: "gif"
            case .tiff: "tiff"
            case .jpeg: "jpg"
            case .bmp: "bmp"
            case .webp: "webp"
            case .heic: "heic"
            case .heif: "heif"
            }
        }
        #expect(imageFormat.fileExtension == expectedFileExtension(imageFormat))
    }
}
