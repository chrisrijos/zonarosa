//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

@objcMembers
public class TSErrorMessageBuilder: TSMessageBuilder {
    public let errorType: TSErrorMessageType
    public var recipientAddress: ZonaRosaServiceAddress?
    public var senderAddress: ZonaRosaServiceAddress?
    public var wasIdentityVerified: Bool = false

    init(
        thread: TSThread,
        errorType: TSErrorMessageType,
    ) {
        self.errorType = errorType

        super.init(
            thread: thread,
            timestamp: nil,
            receivedAtTimestamp: nil,
            messageBody: nil,
            editState: .none,
            expiresInSeconds: nil,
            expireTimerVersion: nil,
            expireStartedAt: nil,
            isSmsMessageRestoredFromBackup: false,
            isViewOnceMessage: false,
            isViewOnceComplete: false,
            wasRemotelyDeleted: false,
            storyAuthorAci: nil,
            storyTimestamp: nil,
            storyReactionEmoji: nil,
            quotedMessage: nil,
            contactShare: nil,
            linkPreview: nil,
            messageSticker: nil,
            giftBadge: nil,
            isPoll: false,
        )
    }

    // MARK: -

    private var hasBuilt = false

    public func build() -> TSErrorMessage {
        if hasBuilt {
            owsFailDebug("Don't build more than once.")
        }

        hasBuilt = true

        return TSErrorMessage(errorMessageWithBuilder: self)
    }
}
