//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import ZonaRosaServiceKit

/// This is absolutely horrible.
///
/// Why does it exist? PaymentsFormat, lives in ZonaRosa, not ZonaRosaServiceKit.
///
/// It can't be easily moved to ZonaRosaServiceKit either; it relies on
/// MobileCoin which isn't a SSK dependency AND it relies on PaymentsImpl
/// which also lives in ZonaRosa.
///
/// But we need PaymentsFormat, and everything it pulls in, to pull the amount string
/// out of an original message. We need the amount to generate the quoted reply; not
/// only is it displayed, it is put into the outgoing proto at send time.
///
/// This extension basically allows the initializers of DraftQuotedReplyModel (which all live
/// in ZonaRosa) to create quoted replies to payment messages but defer everything else
/// to ZonaRosaServiceKit where it should be.
///
/// Note this dance affects message sending; we NEED to create payment message quotes here,
/// in ZonaRosa, so when they are ultimately passed back down to ZonaRosaServiceKit to be sent all we
/// pass along is the already-generated payment amount display string.
extension DraftQuotedReplyModel {

    public static func fromOriginalPaymentMessage(
        _ message: TSMessage,
        tx: DBReadTransaction,
    ) -> DraftQuotedReplyModel? {
        guard let paymentMessage = message as? OWSPaymentMessage else {
            return nil
        }
        let amountString = amountString(paymentMessage, interactionType: message.interactionType, tx: tx)
        return DraftQuotedReplyModel.fromOriginalPaymentMessage(
            message,
            amountString: amountString,
            tx: tx,
        )
    }

    public static func forEditingOriginalPaymentMessage(
        originalMessage: TSMessage,
        replyMessage: TSMessage,
        quotedReply: TSQuotedMessage,
        tx: DBReadTransaction,
    ) -> DraftQuotedReplyModel? {
        guard let paymentMessage = originalMessage as? OWSPaymentMessage else {
            return nil
        }
        let amountString = amountString(paymentMessage, interactionType: originalMessage.interactionType, tx: tx)
        return DraftQuotedReplyModel.forEditingOriginalPaymentMessage(
            originalMessage: originalMessage,
            replyMessage: replyMessage,
            quotedReply: quotedReply,
            amountString: amountString,
            tx: tx,
        )
    }

    private static func amountString(
        _ paymentMessage: OWSPaymentMessage,
        interactionType: OWSInteractionType,
        tx: DBReadTransaction,
    ) -> String {
        return PaymentsFormat.paymentPreviewText(
            paymentMessage: paymentMessage,
            type: interactionType,
            transaction: tx,
        ) ?? OWSLocalizedString(
            "PAYMENTS_PREVIEW_TEXT_UNKNOWN",
            comment: "Payments Preview Text shown in quoted replies, for unknown payments.",
        )
    }
}
