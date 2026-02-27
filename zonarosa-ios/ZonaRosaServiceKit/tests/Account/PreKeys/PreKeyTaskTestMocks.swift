//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import LibZonaRosaClient

@testable import ZonaRosaServiceKit

//
//
// MARK: - Mocks

//
//
extension PreKeyTaskManager {
    enum Mocks {
        typealias APIClient = _PreKeyTaskManager_APIClientMock
        typealias DateProvider = _PreKeyTaskManager_DateProviderMock
        typealias IdentityKeyMismatchManager = _PreKeyTaskManager_IdentityKeyMismatchManagerMock
    }
}

class _PreKeyTaskManager_IdentityKeyMismatchManagerMock: IdentityKeyMismatchManager {
    func recordSuspectedIssueWithPniIdentityKey(tx: DBWriteTransaction) {
    }

    func validateLocalPniIdentityKeyIfNecessary() async {
    }

    var validateIdentityKeyMock: ((_ identity: OWSIdentity) async -> Void)!
    func validateIdentityKey(for identity: OWSIdentity) async {
        await validateIdentityKeyMock!(identity)
    }
}

class _PreKeyTaskManager_DateProviderMock {
    var currentDate: Date = Date()
    func targetDate() -> Date { return currentDate }
}

class _PreKeyTaskManager_APIClientMock: PreKeyTaskAPIClient {
    var currentPreKeyCount: Int?
    var currentPqPreKeyCount: Int?

    var setPreKeysResult: ConsumableMockPromise<Void> = .unset
    var identity: OWSIdentity?
    var signedPreKeyRecord: LibZonaRosaClient.SignedPreKeyRecord?
    var preKeyRecords: [LibZonaRosaClient.PreKeyRecord]?
    var pqLastResortPreKeyRecord: LibZonaRosaClient.KyberPreKeyRecord?
    var pqPreKeyRecords: [LibZonaRosaClient.KyberPreKeyRecord]?
    var auth: ChatServiceAuth?

    func getAvailablePreKeys(for identity: OWSIdentity) async throws -> (ecCount: Int, pqCount: Int) {
        return (currentPreKeyCount!, currentPqPreKeyCount!)
    }

    func registerPreKeys(
        for identity: OWSIdentity,
        signedPreKeyRecord: LibZonaRosaClient.SignedPreKeyRecord?,
        preKeyRecords: [LibZonaRosaClient.PreKeyRecord]?,
        pqLastResortPreKeyRecord: LibZonaRosaClient.KyberPreKeyRecord?,
        pqPreKeyRecords: [LibZonaRosaClient.KyberPreKeyRecord]?,
        auth: ChatServiceAuth,
    ) async throws {
        try await setPreKeysResult.consumeIntoPromise().awaitable()

        self.identity = identity
        self.signedPreKeyRecord = signedPreKeyRecord
        self.preKeyRecords = preKeyRecords
        self.pqLastResortPreKeyRecord = pqLastResortPreKeyRecord
        self.pqPreKeyRecords = pqPreKeyRecords
        self.auth = auth
    }
}
