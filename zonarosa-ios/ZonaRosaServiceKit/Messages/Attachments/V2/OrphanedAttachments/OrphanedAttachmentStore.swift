//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import GRDB

/// Wrapper around OrphanedAttachmentRecord table for reads/writes.
public struct OrphanedAttachmentStore {
    public init() {}

    public func orphanAttachmentExists(
        with id: OrphanedAttachmentRecord.RowId,
        tx: DBReadTransaction,
    ) -> Bool {
        return failIfThrows {
            return try OrphanedAttachmentRecord.exists(tx.database, key: id)
        }
    }
}
