//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import GRDB
public import LibZonaRosaClient

public struct RecipientDatabaseTable {
    public init() {}

    func fetchRecipient(contactThread: TSContactThread, tx: DBReadTransaction) -> ZonaRosaRecipient? {
        return fetchServiceIdAndRecipient(contactThread: contactThread, tx: tx)
            .flatMap { _, recipient in recipient }
    }

    func fetchServiceId(contactThread: TSContactThread, tx: DBReadTransaction) -> ServiceId? {
        return fetchServiceIdAndRecipient(contactThread: contactThread, tx: tx)
            .map { serviceId, _ in serviceId }
    }

    /// Fetch the `ServiceId` for the owner of this contact thread, and its
    /// corresponding `ZonaRosaRecipient` if one exists.
    private func fetchServiceIdAndRecipient(
        contactThread: TSContactThread,
        tx: DBReadTransaction,
    ) -> (ServiceId, ZonaRosaRecipient?)? {
        let threadServiceId = contactThread.contactUUID.flatMap { try? ServiceId.parseFrom(serviceIdString: $0) }

        // If there's an ACI, it's *definitely* correct, and it's definitely the
        // owner, so we can return early without issuing any queries.
        if let aci = threadServiceId as? Aci {
            let ownedByRecipient = fetchRecipient(serviceId: aci, transaction: tx)

            return (aci, ownedByRecipient)
        }

        // Otherwise, we need to figure out which recipient "owns" this thread. If
        // the thread has a phone number but there's no ZonaRosaRecipient with that
        // phone number, we'll return nil (even if the thread has a PNI). This is
        // intentional. In this case, the phone number takes precedence, and this
        // PNI definitely isn’t associated with this phone number. This situation
        // should be impossible because ThreadMerger should keep these values in
        // sync. (It's pre-ThreadMerger threads that might be wrong, and PNIs were
        // introduced after ThreadMerger.)
        if let phoneNumber = contactThread.contactPhoneNumber {
            let ownedByRecipient = fetchRecipient(phoneNumber: phoneNumber, transaction: tx)
            let ownedByServiceId = ownedByRecipient?.aci ?? ownedByRecipient?.pni

            return ownedByServiceId.map { ($0, ownedByRecipient) }
        }

        if let pni = threadServiceId as? Pni {
            let ownedByRecipient = fetchRecipient(serviceId: pni, transaction: tx)
            let ownedByServiceId = ownedByRecipient?.aci ?? ownedByRecipient?.pni ?? pni

            return (ownedByServiceId, ownedByRecipient)
        }

        return nil
    }

    // MARK: -

    public func fetchRecipient(address: ZonaRosaServiceAddress, tx: DBReadTransaction) -> ZonaRosaRecipient? {
        return
            address.serviceId.flatMap({ fetchRecipient(serviceId: $0, transaction: tx) })
                ?? address.phoneNumber.flatMap({ fetchRecipient(phoneNumber: $0, transaction: tx) })

    }

    public func fetchAuthorRecipient(incomingMessage: TSIncomingMessage, tx: DBReadTransaction) -> ZonaRosaRecipient? {
        return fetchRecipient(address: incomingMessage.authorAddress, tx: tx)
    }

    public func fetchRecipient(rowId: Int64, tx: DBReadTransaction) -> ZonaRosaRecipient? {
        return failIfThrows {
            return try ZonaRosaRecipient.fetchOne(tx.database, key: rowId)
        }
    }

    public func fetchRecipient(uniqueId: String, tx: DBReadTransaction) -> ZonaRosaRecipient? {
        let sql = "SELECT * FROM \(ZonaRosaRecipient.databaseTableName) WHERE \(zonarosaRecipientColumn: .uniqueId) = ?"
        return failIfThrows {
            return try ZonaRosaRecipient.fetchOne(tx.database, sql: sql, arguments: [uniqueId])
        }
    }

    public func fetchRecipient(serviceId: ServiceId, transaction tx: DBReadTransaction) -> ZonaRosaRecipient? {
        let serviceIdColumn: ZonaRosaRecipient.CodingKeys = {
            switch serviceId.kind {
            case .aci: return .aciString
            case .pni: return .pni
            }
        }()
        let sql = "SELECT * FROM \(ZonaRosaRecipient.databaseTableName) WHERE \(zonarosaRecipientColumn: serviceIdColumn) = ?"
        return failIfThrows {
            return try ZonaRosaRecipient.fetchOne(tx.database, sql: sql, arguments: [serviceId.serviceIdUppercaseString])
        }
    }

    public func fetchRecipient(phoneNumber: String, transaction tx: DBReadTransaction) -> ZonaRosaRecipient? {
        let sql = "SELECT * FROM \(ZonaRosaRecipient.databaseTableName) WHERE \(zonarosaRecipientColumn: .phoneNumber) = ?"
        return failIfThrows {
            return try ZonaRosaRecipient.fetchOne(tx.database, sql: sql, arguments: [phoneNumber])
        }
    }

    public func enumerateAll(tx: DBReadTransaction, block: (ZonaRosaRecipient) -> Void) {
        failIfThrows {
            let cursor = try ZonaRosaRecipient.fetchCursor(tx.database)
            var hasMore = true
            while hasMore {
                try autoreleasepool {
                    guard let recipient = try cursor.next() else {
                        hasMore = false
                        return
                    }
                    block(recipient)
                }
            }
        }
    }

    public func fetchWhitelistedRecipients(tx: DBReadTransaction) -> [ZonaRosaRecipient] {
        let fetchRequest = ZonaRosaRecipient.filter(
            Column(ZonaRosaRecipient.CodingKeys.status.rawValue) == ZonaRosaRecipient.Status.whitelisted.rawValue,
        )
        return failIfThrows { try fetchRequest.fetchAll(tx.database) }
    }

    public func fetchAllPhoneNumbers(tx: DBReadTransaction) -> [String: Bool] {
        var result = [String: Bool]()
        enumerateAll(tx: tx) { zonarosaRecipient in
            guard let phoneNumber = zonarosaRecipient.phoneNumber?.stringValue else {
                return
            }
            result[phoneNumber] = zonarosaRecipient.isRegistered
        }
        return result
    }

    public func updateRecipient(_ zonarosaRecipient: ZonaRosaRecipient, transaction: DBWriteTransaction) {
        failIfThrows {
            try zonarosaRecipient.update(transaction.database)
        }
    }

    public func removeRecipient(_ zonarosaRecipient: ZonaRosaRecipient, transaction: DBWriteTransaction) {
        failIfThrows {
            try zonarosaRecipient.delete(transaction.database)
        }
    }
}
