//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import ZonaRosaServiceKit

struct ChatListFilterStore {
    private enum Constants {
        static let collectionName = "ChatListFilterStore"
        static let inboxFilterKey = "inboxFilter"
    }

    private let store = KeyValueStore(collection: Constants.collectionName)

    func inboxFilter(transaction: some DBReadTransaction) -> InboxFilter? {
        let rawValue = store.getInt(Constants.inboxFilterKey, defaultValue: 0, transaction: transaction)
        guard let inboxFilter = InboxFilter(rawValue: rawValue) else {
            owsFailDebug("Unknown inbox filter (rawValue \(rawValue))")
            return nil
        }
        return inboxFilter
    }

    func setInboxFilter(_ inboxFilter: InboxFilter, transaction: some DBWriteTransaction) {
        store.setInt(inboxFilter.rawValue, key: Constants.inboxFilterKey, transaction: transaction)
    }
}
