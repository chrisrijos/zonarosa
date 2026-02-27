//
// Copyright 2019 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import LibZonaRosaClient

public typealias RecipientUniqueId = String

public enum RecipientIdError: Error, IsRetryableProvider {
    /// We can't use the Pni because it's been replaced by an Aci.
    case mustNotUsePniBecauseAciExists

    public var isRetryableProvider: Bool {
        // Allow retries so that we send to the Aci instead of the Pni.
        return true
    }
}

public final class RecipientIdFinder {
    private let recipientDatabaseTable: RecipientDatabaseTable
    private let recipientFetcher: RecipientFetcher

    public init(
        recipientDatabaseTable: RecipientDatabaseTable,
        recipientFetcher: RecipientFetcher,
    ) {
        self.recipientFetcher = recipientFetcher
        self.recipientDatabaseTable = recipientDatabaseTable
    }

    public func recipientUniqueId(for serviceId: ServiceId, tx: DBReadTransaction) -> Result<RecipientUniqueId, RecipientIdError>? {
        guard let recipient = recipientDatabaseTable.fetchRecipient(serviceId: serviceId, transaction: tx) else {
            return nil
        }
        return validateRecipient(recipient, for: serviceId).map(\.uniqueId)
    }

    public func recipientUniqueId(for address: ZonaRosaServiceAddress, tx: DBReadTransaction) -> Result<RecipientUniqueId, RecipientIdError>? {
        guard let recipient = recipientDatabaseTable.fetchRecipient(address: address, tx: tx) else {
            return nil
        }
        return validateRecipient(recipient, for: address.serviceId).map(\.uniqueId)
    }

    public func ensureRecipientUniqueId(for serviceId: ServiceId, tx: DBWriteTransaction) -> Result<RecipientUniqueId, RecipientIdError> {
        return ensureRecipient(for: serviceId, tx: tx).map(\.uniqueId)
    }

    public func recipientId(for serviceId: ServiceId, tx: DBReadTransaction) -> Result<ZonaRosaRecipient.RowId, RecipientIdError>? {
        guard let recipient = recipientDatabaseTable.fetchRecipient(serviceId: serviceId, transaction: tx) else {
            return nil
        }
        return validateRecipient(recipient, for: serviceId).map(\.id)
    }

    public func recipientId(for address: ZonaRosaServiceAddress, tx: DBReadTransaction) -> Result<ZonaRosaRecipient.RowId, RecipientIdError>? {
        guard let recipient = recipientDatabaseTable.fetchRecipient(address: address, tx: tx) else {
            return nil
        }
        return validateRecipient(recipient, for: address.serviceId).map(\.id)
    }

    public func ensureRecipientId(for serviceId: ServiceId, tx: DBWriteTransaction) -> Result<ZonaRosaRecipient.RowId, RecipientIdError> {
        return ensureRecipient(for: serviceId, tx: tx).map(\.id)
    }

    public func ensureRecipient(for serviceId: ServiceId, tx: DBWriteTransaction) -> Result<ZonaRosaRecipient, RecipientIdError> {
        let recipient = recipientFetcher.fetchOrCreate(serviceId: serviceId, tx: tx)
        return validateRecipient(recipient, for: serviceId)
    }

    private func validateRecipient(
        _ recipient: ZonaRosaRecipient,
        for serviceId: ServiceId?,
    ) -> Result<ZonaRosaRecipient, RecipientIdError> {
        if serviceId is Pni, recipient.aciString != nil {
            return .failure(.mustNotUsePniBecauseAciExists)
        }
        return .success(recipient)
    }
}
