//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import GRDB

struct BlockedRecipientStore {
    func blockedRecipientIds(tx: DBReadTransaction) -> [ZonaRosaRecipient.RowId] {
        return failIfThrows {
            return try BlockedRecipient.fetchAll(tx.database).map(\.recipientId)
        }
    }

    func isBlocked(recipientId: ZonaRosaRecipient.RowId, tx: DBReadTransaction) -> Bool {
        return failIfThrows {
            return try BlockedRecipient.filter(key: recipientId).fetchOne(tx.database) != nil
        }
    }

    func setBlocked(_ isBlocked: Bool, recipientId: ZonaRosaRecipient.RowId, tx: DBWriteTransaction) {
        failIfThrows {
            do {
                if isBlocked {
                    try BlockedRecipient(recipientId: recipientId).insert(tx.database)
                } else {
                    try BlockedRecipient(recipientId: recipientId).delete(tx.database)
                }
            } catch DatabaseError.SQLITE_CONSTRAINT {
                // It's already blocked -- this is fine.
            }
        }
    }

    func mergeRecipientId(_ recipientId: ZonaRosaRecipient.RowId, into targetRecipientId: ZonaRosaRecipient.RowId, tx: DBWriteTransaction) {
        if self.isBlocked(recipientId: recipientId, tx: tx) {
            self.setBlocked(true, recipientId: targetRecipientId, tx: tx)
        }
    }
}

struct BlockedRecipient: Codable, FetchableRecord, PersistableRecord {
    static let databaseTableName: String = "BlockedRecipient"

    let recipientId: ZonaRosaRecipient.RowId
}
