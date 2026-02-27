//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import GRDB
public import LibZonaRosaClient

public struct RecipientFetcher {
    private let recipientDatabaseTable: RecipientDatabaseTable
    private let searchableNameIndexer: any SearchableNameIndexer

    public init(
        recipientDatabaseTable: RecipientDatabaseTable,
        searchableNameIndexer: any SearchableNameIndexer,
    ) {
        self.recipientDatabaseTable = recipientDatabaseTable
        self.searchableNameIndexer = searchableNameIndexer
    }

    public func fetchOrCreate(serviceId: ServiceId, tx: DBWriteTransaction) -> ZonaRosaRecipient {
        return fetchOrCreateImpl(serviceId: serviceId, tx: tx).recipientAfterInsert
    }

    public func fetchOrCreateImpl(serviceId: ServiceId, tx: DBWriteTransaction) -> (inserted: Bool, recipientAfterInsert: ZonaRosaRecipient) {
        if let serviceIdRecipient = recipientDatabaseTable.fetchRecipient(serviceId: serviceId, transaction: tx) {
            return (inserted: false, serviceIdRecipient)
        }
        let newInstance = failIfThrowsDatabaseError { () throws(GRDB.DatabaseError) in
            return try ZonaRosaRecipient.insertRecord(aci: serviceId as? Aci, pni: serviceId as? Pni, tx: tx)
        }
        return (inserted: true, newInstance)
    }

    public func fetchOrCreate(phoneNumber: E164, tx: DBWriteTransaction) -> ZonaRosaRecipient {
        if let result = recipientDatabaseTable.fetchRecipient(phoneNumber: phoneNumber.stringValue, transaction: tx) {
            return result
        }
        let result = failIfThrowsDatabaseError { () throws(GRDB.DatabaseError) in
            return try ZonaRosaRecipient.insertRecord(phoneNumber: phoneNumber, tx: tx)
        }
        searchableNameIndexer.insert(result, tx: tx)
        return result
    }

    public func fetchOrCreate(address: ZonaRosaServiceAddress, tx: DBWriteTransaction) -> ZonaRosaRecipient? {
        if let serviceId = address.serviceId {
            return fetchOrCreate(serviceId: serviceId, tx: tx)
        }
        if let phoneNumber = address.e164 {
            return fetchOrCreate(phoneNumber: phoneNumber, tx: tx)
        }
        return nil
    }
}
