//
// Copyright 2020 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import LibZonaRosaClient
public import ZonaRosaServiceKit
public import ZonaRosaUI

extension ConversationViewController: BodyRangesTextViewDelegate {
    var supportsMentions: Bool { thread.allowsMentionSend }

    public func textViewDidBeginTypingMention(_ textView: BodyRangesTextView) {}

    public func textViewDidEndTypingMention(_ textView: BodyRangesTextView) {}

    public func textViewMentionPickerParentView(_ textView: BodyRangesTextView) -> UIView? {
        view
    }

    public func textViewMentionPickerReferenceView(_ textView: BodyRangesTextView) -> UIView? {
        bottomBarContainer
    }

    public func textViewMentionPickerPossibleAcis(_ textView: BodyRangesTextView, tx: DBReadTransaction) -> [Aci] {
        supportsMentions ? thread.recipientAddresses(with: tx).compactMap(\.aci) : []
    }

    public func textViewMentionCacheInvalidationKey(_ textView: BodyRangesTextView) -> String {
        return thread.uniqueId
    }

    public func textViewDisplayConfiguration(_ textView: BodyRangesTextView) -> HydratedMessageBody.DisplayConfiguration {
        return .composing(textViewColor: textView.textColor)
    }

    public func mentionPickerStyle(_ textView: BodyRangesTextView) -> MentionPickerStyle {
        return .default
    }

    public func textViewDidInsertMemoji(_ memojiGlyph: OWSAdaptiveImageGlyph) {
        do {
            let attachmentLimits = OutgoingAttachmentLimits.currentLimits()
            self.didPasteAttachments(
                [try PasteboardAttachment.loadPreviewableMemojiAttachment(fromMemojiGlyph: memojiGlyph)],
                attachmentLimits: attachmentLimits,
            )
        } catch {
            self.showErrorAlert(attachmentError: error as? ZonaRosaAttachmentError)
        }
    }
}
