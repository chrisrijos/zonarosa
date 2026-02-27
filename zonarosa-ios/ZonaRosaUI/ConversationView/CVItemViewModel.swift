//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import ZonaRosaServiceKit

public protocol CVItemViewModel: AnyObject {
    var interaction: TSInteraction { get }
    var contactShare: ContactShareViewModel? { get }
    var linkPreview: OWSLinkPreview? { get }
    var stickerAttachment: AttachmentStream? { get }
    var stickerMetadata: (any StickerMetadata)? { get }
    var isGiftBadge: Bool { get }
    var hasRenderableContent: Bool { get }
}
