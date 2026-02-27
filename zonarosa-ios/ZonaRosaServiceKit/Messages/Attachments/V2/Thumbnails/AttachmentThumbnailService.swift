//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public protocol AttachmentThumbnailService {

    func thumbnailImage(
        for attachmentStream: AttachmentStream,
        quality: AttachmentThumbnailQuality,
    ) async -> UIImage?

    func thumbnailImageSync(
        for attachmentStream: AttachmentStream,
        quality: AttachmentThumbnailQuality,
    ) -> UIImage?

    func backupThumbnailData(image: UIImage) throws -> Data
}
