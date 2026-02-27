//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

#if TESTABLE_BUILD

open class MockAttachmentThumbnailService: AttachmentThumbnailService {

    public init() {}

    open func thumbnailImage(
        for attachmentStream: AttachmentStream,
        quality: AttachmentThumbnailQuality,
    ) async -> UIImage? {
        return nil
    }

    open func thumbnailImageSync(
        for attachmentStream: AttachmentStream,
        quality: AttachmentThumbnailQuality,
    ) -> UIImage? {
        return nil
    }

    open func backupThumbnailData(image: UIImage) throws -> Data {
        return Data()
    }
}

#endif
