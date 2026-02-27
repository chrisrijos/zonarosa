//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

extension TSInfoMessage {
    /// Creates a `TSInfoMessage` indicating that a contact was hidden.
    ///
    /// - Note
    /// The presence and placement of this info message affects message-request
    /// state for its thread.
    ///
    /// - SeeAlso ``RecipientHidingManager/isHiddenRecipientThreadInMessageRequest(hiddenRecipient:contactThread:tx:)``
    /// - SeeAlso ``ThreadFinder/hasPendingMessageRequest(thread:transaction:)``
    static func makeForContactHidden(contactThread: TSContactThread) -> TSInfoMessage {
        return TSInfoMessage(thread: contactThread, messageType: .recipientHidden)
    }
}
