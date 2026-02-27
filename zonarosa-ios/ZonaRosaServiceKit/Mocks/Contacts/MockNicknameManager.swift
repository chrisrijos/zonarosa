//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

#if TESTABLE_BUILD

public class MockNicknameManager: NicknameManager {
    private var mockNicknames: [Int64: NicknameRecord] = [:]

    public func fetchNickname(for recipient: ZonaRosaRecipient, tx: DBReadTransaction) -> NicknameRecord? {
        return mockNicknames[recipient.id]
    }

    public func createOrUpdate(
        nicknameRecord: NicknameRecord,
        updateStorageServiceFor recipientUniqueId: RecipientUniqueId?,
        tx: DBWriteTransaction,
    ) {
        self.insert(nicknameRecord, tx: tx)
    }

    func insert(_ nicknameRecord: NicknameRecord, tx: DBWriteTransaction) {
        mockNicknames[nicknameRecord.recipientRowID] = nicknameRecord
    }

    public func deleteNickname(
        recipientRowID: Int64,
        updateStorageServiceFor recipientUniqueId: RecipientUniqueId?,
        tx: DBWriteTransaction,
    ) {
        mockNicknames.removeValue(forKey: recipientRowID)
    }
}

#endif
