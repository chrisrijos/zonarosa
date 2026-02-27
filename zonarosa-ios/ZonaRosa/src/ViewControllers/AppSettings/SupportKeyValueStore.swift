//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import ZonaRosaServiceKit

public class SupportKeyValueStore {
    private enum StoreKeys {
        static let lastChallengeDateKey: String = "lastChallengeDateKey"
    }

    private let kvStore: KeyValueStore

    public init() {
        self.kvStore = KeyValueStore(collection: "ComposeSupportEmailOperation")
    }

    public func setLastChallengeDate(
        value: Date,
        transaction: DBWriteTransaction,
    ) {
        kvStore.setDate(
            value,
            key: StoreKeys.lastChallengeDateKey,
            transaction: transaction,
        )
    }

    public func lastChallengeWithinTimeframe(
        transaction: DBReadTransaction,
        lastChallengeFloor: Date,
    ) -> Bool {
        return kvStore.getDate(StoreKeys.lastChallengeDateKey, transaction: transaction) ?? Date.distantPast > lastChallengeFloor
    }
}
