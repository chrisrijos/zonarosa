//
// Copyright 2018 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import ZonaRosaServiceKit

public class TypingIndicatorInteraction: TSInteraction {
    public static let TypingIndicatorId = "TypingIndicator"

    override public var isDynamicInteraction: Bool {
        true
    }

    override public var interactionType: OWSInteractionType {
        .typingIndicator
    }

    public let address: ZonaRosaServiceAddress

    public init(thread: TSThread, timestamp: UInt64, address: ZonaRosaServiceAddress) {
        self.address = address

        super.init(
            customUniqueId: TypingIndicatorInteraction.TypingIndicatorId,
            timestamp: timestamp,
            receivedAtTimestamp: 0,
            thread: thread,
        )
    }

    override public var shouldBeSaved: Bool {
        return false
    }

    override public func anyWillInsert(with transaction: DBWriteTransaction) {
        owsFailDebug("The transient interaction should not be saved in the database.")
    }
}
