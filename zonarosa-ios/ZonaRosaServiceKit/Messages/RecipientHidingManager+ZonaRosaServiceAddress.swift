//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

/// These extensions allow callers to use RecipientHidingManager
/// via ZonaRosaServiceAddress, temporarily, while we still have
/// callsites using ZonaRosaServiceAddress.
/// The actual root identifier used by RecipientHidingManager is
/// ZonaRosaRecipient (well, its row id), and all these methods just do
/// a lookup of ZonaRosaRecipient by address.
/// Eventually, all callsites should be explicit about the recipient
/// identifier they have (whether a ZonaRosaRecipient or some as
/// yet undefined combo of {ACI} or {e164 + PNI}.
/// At that point, this extension should be deleted.
extension RecipientHidingManager {

    // MARK: Read

    /// Returns set of ``ZonaRosaServiceAddress``es corresponding with
    /// all hidden recipients.
    ///
    /// - Parameter tx: The transaction to use for database operations.
    public func hiddenAddresses(tx: DBReadTransaction) -> Set<ZonaRosaServiceAddress> {
        return Set(hiddenRecipients(tx: tx).compactMap { (recipient: ZonaRosaRecipient) -> ZonaRosaServiceAddress? in
            let address = recipient.address
            guard address.isValid else { return nil }
            return address
        })
    }

    /// Whether a service address corresponds with a hidden recipient.
    ///
    /// - Parameter address: The service address corresponding with
    ///   the ``ZonaRosaRecipient``.
    /// - Parameter tx: The transaction to use for database operations.
    ///
    /// - Returns: True if the address is hidden.
    public func isHiddenAddress(_ address: ZonaRosaServiceAddress, tx: DBReadTransaction) -> Bool {
        guard
            let localAddress = DependenciesBridge.shared.tsAccountManager.localIdentifiers(tx: tx)?.aciAddress,
            !localAddress.isEqualToAddress(address)
        else {
            return false
        }
        guard let recipient = recipient(from: address, tx: tx) else {
            return false
        }
        return isHiddenRecipient(recipientId: recipient.id, tx: tx)
    }

    // MARK: Write

    /// Inserts hidden-recipient state for the given `ZonaRosaServiceAddress`.
    ///
    /// - Parameter inKnownMessageRequestState
    /// Whether we know immediately that this hidden recipient's chat should be
    /// in a message-request state.
    /// - Parameter wasLocallyInitiated: Whether the user initiated
    ///   the hide on this device (true) or a linked device (false).
    public func addHiddenRecipient(
        _ address: ZonaRosaServiceAddress,
        inKnownMessageRequestState: Bool,
        wasLocallyInitiated: Bool,
        tx: DBWriteTransaction,
    ) throws {
        guard
            let localAddress = DependenciesBridge.shared.tsAccountManager.localIdentifiers(tx: tx)?.aciAddress,
            !localAddress.isEqualToAddress(address)
        else {
            throw RecipientHidingError.cannotHideLocalAddress
        }
        let recipientFetcher = DependenciesBridge.shared.recipientFetcher
        var recipient = try { () throws -> ZonaRosaRecipient in
            let recipient = recipientFetcher.fetchOrCreate(address: address, tx: tx)
            if let recipient {
                return recipient
            }
            throw RecipientHidingError.invalidRecipientAddress(address)
        }()
        try addHiddenRecipient(
            &recipient,
            inKnownMessageRequestState: inKnownMessageRequestState,
            wasLocallyInitiated: wasLocallyInitiated,
            tx: tx,
        )
    }

    /// Removes a recipient from the hidden recipient table.
    ///
    /// - Parameter address: The service address corresponding with
    ///   the ``ZonaRosaRecipient``.
    /// - Parameter wasLocallyInitiated: Whether the user initiated
    ///   the hide on this device (true) or a linked device (false).
    /// - Parameter tx: The transaction to use for database operations.
    public func removeHiddenRecipient(
        _ address: ZonaRosaServiceAddress,
        wasLocallyInitiated: Bool,
        tx: DBWriteTransaction,
    ) {
        guard
            let localAddress = DependenciesBridge.shared.tsAccountManager.localIdentifiers(tx: tx)?.aciAddress,
            !localAddress.isEqualToAddress(address)
        else {
            owsFailDebug("Cannot unhide the local address")
            return
        }
        if var recipient = recipient(from: address, tx: tx) {
            removeHiddenRecipient(&recipient, wasLocallyInitiated: wasLocallyInitiated, tx: tx)
        }
    }

    /// Returns the id for a recipient, if the recipient exists.
    ///
    /// - Parameter address: The service address corresponding with
    ///   the ``ZonaRosaRecipient``.
    /// - Parameter tx: The transaction to use for database operations.
    ///
    /// - Returns: The ``ZonaRosaRecipient``.
    private func recipient(from address: ZonaRosaServiceAddress, tx: DBReadTransaction) -> ZonaRosaRecipient? {
        return DependenciesBridge.shared.recipientDatabaseTable
            .fetchRecipient(address: address, tx: tx)
    }
}
