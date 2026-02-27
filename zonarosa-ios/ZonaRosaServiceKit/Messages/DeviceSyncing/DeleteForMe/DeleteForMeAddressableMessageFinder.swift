//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

protocol DeleteForMeAddressableMessageFinder {
    func findLocalMessage(
        threadUniqueId: String,
        addressableMessage: DeleteForMeSyncMessage.Incoming.AddressableMessage,
        tx: DBReadTransaction,
    ) -> TSMessage?

    func threadContainsAnyAddressableMessages(
        threadUniqueId: String,
        tx: DBReadTransaction,
    ) -> Bool
}

// MARK: -

final class DeleteForMeAddressableMessageFinderImpl: DeleteForMeAddressableMessageFinder {
    private let tsAccountManager: TSAccountManager

    init(tsAccountManager: TSAccountManager) {
        self.tsAccountManager = tsAccountManager
    }

    func findLocalMessage(
        threadUniqueId: String,
        addressableMessage: DeleteForMeSyncMessage.Incoming.AddressableMessage,
        tx: DBReadTransaction,
    ) -> TSMessage? {
        let authorAddress: ZonaRosaServiceAddress
        switch addressableMessage.author {
        case .localUser:
            guard let localAddress = tsAccountManager.localIdentifiers(tx: tx)?.aciAddress else {
                return nil
            }
            authorAddress = localAddress
        case .otherUser(let zonarosaRecipient):
            authorAddress = zonarosaRecipient.address
        }

        return InteractionFinder.findMessage(
            withTimestamp: addressableMessage.sentTimestamp,
            threadId: threadUniqueId,
            author: authorAddress,
            transaction: tx,
        )
    }

    func threadContainsAnyAddressableMessages(
        threadUniqueId: String,
        tx: DBReadTransaction,
    ) -> Bool {
        var foundAddressableMessage = false

        do {
            try DeleteForMeMostRecentAddressableMessageCursor(
                threadUniqueId: threadUniqueId,
                sdsTx: tx,
            ).iterate { interaction in
                owsPrecondition(
                    interaction is TSIncomingMessage || interaction is TSOutgoingMessage,
                    "Unexpected interaction type! \(type(of: interaction))",
                )

                foundAddressableMessage = true
                return false
            }
        } catch {
            owsFailDebug("Failed to enumerate interactions!")
            return false
        }

        return foundAddressableMessage
    }
}

// MARK: - Mock

#if TESTABLE_BUILD

open class MockDeleteForMeAddressableMessageFinder: DeleteForMeAddressableMessageFinder {
    func findLocalMessage(threadUniqueId: String, addressableMessage: DeleteForMeSyncMessage.Incoming.AddressableMessage, tx: DBReadTransaction) -> TSMessage? {
        return nil
    }

    func threadContainsAnyAddressableMessages(threadUniqueId: String, tx: DBReadTransaction) -> Bool {
        return false
    }
}

#endif
