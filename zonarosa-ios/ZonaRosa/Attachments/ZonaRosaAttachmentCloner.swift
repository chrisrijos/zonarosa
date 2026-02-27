//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaServiceKit
import ZonaRosaUI

enum ZonaRosaAttachmentCloner {
    static func cloneAsZonaRosaAttachment(
        attachment: ReferencedAttachmentStream,
        attachmentLimits: OutgoingAttachmentLimits,
    ) throws -> PreviewableAttachment {
        guard let dataUTI = MimeTypeUtil.utiTypeForMimeType(attachment.attachmentStream.mimeType) else {
            throw OWSAssertionError("Missing dataUTI.")
        }

        let decryptedCopyUrl = try attachment.attachmentStream.makeDecryptedCopy(
            filename: attachment.reference.sourceFilename,
        )

        let decryptedDataSource = DataSourcePath(fileUrl: decryptedCopyUrl, ownership: .owned)
        decryptedDataSource.sourceFilename = attachment.reference.sourceFilename

        let result: PreviewableAttachment
        switch attachment.reference.renderingFlag {
        case .default:
            result = try PreviewableAttachment.buildAttachment(dataSource: decryptedDataSource, dataUTI: dataUTI, attachmentLimits: attachmentLimits)
        case .voiceMessage:
            result = try PreviewableAttachment.voiceMessageAttachment(dataSource: decryptedDataSource, dataUTI: dataUTI, attachmentLimits: attachmentLimits)
        case .borderless:
            result = try PreviewableAttachment.imageAttachment(dataSource: decryptedDataSource, dataUTI: dataUTI)
            result.rawValue.isBorderless = true
        case .shouldLoop:
            result = try PreviewableAttachment.buildAttachment(dataSource: decryptedDataSource, dataUTI: dataUTI, attachmentLimits: attachmentLimits)
            result.rawValue.isLoopingVideo = true
        }
        return result
    }
}
