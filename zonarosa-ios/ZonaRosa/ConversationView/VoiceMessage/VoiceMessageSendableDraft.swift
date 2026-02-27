//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import CoreServices
import Foundation
import ZonaRosaServiceKit
import ZonaRosaUI
import UniformTypeIdentifiers

protocol VoiceMessageSendableDraft {
    func prepareForSending() throws -> URL
}

extension VoiceMessageSendableDraft {
    private func userVisibleFilename(currentDate: Date) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd-HH-mm-ss-SSS"
        let dateString = dateFormatter.string(from: Date())
        return String(
            format: "zonarosa-%@.%@",
            dateString,
            VoiceMessageConstants.fileExtension,
        )
    }

    func prepareAttachment(attachmentLimits: OutgoingAttachmentLimits) throws -> PreviewableAttachment {
        let attachmentUrl = try prepareForSending()

        let dataSource = DataSourcePath(fileUrl: attachmentUrl, ownership: .owned)
        dataSource.sourceFilename = userVisibleFilename(currentDate: Date())

        return try PreviewableAttachment.voiceMessageAttachment(
            dataSource: dataSource,
            dataUTI: UTType.mpeg4Audio.identifier,
            attachmentLimits: attachmentLimits,
        )
    }
}
