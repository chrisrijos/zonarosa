//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import LibZonaRosaClient

protocol PreKeyTaskAPIClient {
    func getAvailablePreKeys(
        for identity: OWSIdentity,
    ) async throws -> (ecCount: Int, pqCount: Int)

    func registerPreKeys(
        for identity: OWSIdentity,
        signedPreKeyRecord: LibZonaRosaClient.SignedPreKeyRecord?,
        preKeyRecords: [LibZonaRosaClient.PreKeyRecord]?,
        pqLastResortPreKeyRecord: LibZonaRosaClient.KyberPreKeyRecord?,
        pqPreKeyRecords: [LibZonaRosaClient.KyberPreKeyRecord]?,
        auth: ChatServiceAuth,
    ) async throws
}

struct PreKeyTaskAPIClientImpl: PreKeyTaskAPIClient {
    private let networkManager: NetworkManager

    init(networkManager: NetworkManager) {
        self.networkManager = networkManager
    }

    func getAvailablePreKeys(
        for identity: OWSIdentity,
    ) async throws -> (ecCount: Int, pqCount: Int) {
        let request = OWSRequestFactory.availablePreKeysCountRequest(for: identity)
        let response = try await networkManager.asyncRequest(request)

        guard let params = response.responseBodyParamParser else {
            throw OWSAssertionError("Missing or invalid JSON.")
        }

        let ecCount: Int = try params.required(key: "count")
        let pqCount: Int = try params.optional(key: "pqCount") ?? 0

        return (ecCount, pqCount)
    }

    func registerPreKeys(
        for identity: OWSIdentity,
        signedPreKeyRecord: LibZonaRosaClient.SignedPreKeyRecord?,
        preKeyRecords: [LibZonaRosaClient.PreKeyRecord]?,
        pqLastResortPreKeyRecord: LibZonaRosaClient.KyberPreKeyRecord?,
        pqPreKeyRecords: [LibZonaRosaClient.KyberPreKeyRecord]?,
        auth: ChatServiceAuth,
    ) async throws {
        let request = OWSRequestFactory.registerPrekeysRequest(
            identity: identity,
            signedPreKeyRecord: signedPreKeyRecord,
            prekeyRecords: preKeyRecords,
            pqLastResortPreKeyRecord: pqLastResortPreKeyRecord,
            pqPreKeyRecords: pqPreKeyRecords,
            auth: auth,
        )

        _ = try await networkManager.asyncRequest(request)
    }
}
