//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import LibZonaRosaClient

public protocol PreKeyUploadBundle {
    var identity: OWSIdentity { get }
    func getSignedPreKey() -> LibZonaRosaClient.SignedPreKeyRecord?
    func getPreKeyRecords() -> [LibZonaRosaClient.PreKeyRecord]?
    func getLastResortPreKey() -> LibZonaRosaClient.KyberPreKeyRecord?
    func getPqPreKeyRecords() -> [LibZonaRosaClient.KyberPreKeyRecord]?
}

extension PreKeyUploadBundle {
    func isEmpty() -> Bool {
        if
            getPreKeyRecords() == nil,
            getSignedPreKey() == nil,
            getLastResortPreKey() == nil,
            getPqPreKeyRecords() == nil
        {
            return true
        }
        return false
    }
}

public final class PartialPreKeyUploadBundle: PreKeyUploadBundle {
    public let identity: OWSIdentity
    public let signedPreKey: LibZonaRosaClient.SignedPreKeyRecord?
    public let preKeyRecords: [LibZonaRosaClient.PreKeyRecord]?
    public let lastResortPreKey: LibZonaRosaClient.KyberPreKeyRecord?
    public let pqPreKeyRecords: [LibZonaRosaClient.KyberPreKeyRecord]?

    init(
        identity: OWSIdentity,
        signedPreKey: LibZonaRosaClient.SignedPreKeyRecord? = nil,
        preKeyRecords: [LibZonaRosaClient.PreKeyRecord]? = nil,
        lastResortPreKey: LibZonaRosaClient.KyberPreKeyRecord? = nil,
        pqPreKeyRecords: [LibZonaRosaClient.KyberPreKeyRecord]? = nil,
    ) {
        self.identity = identity
        self.signedPreKey = signedPreKey
        self.preKeyRecords = preKeyRecords
        self.lastResortPreKey = lastResortPreKey
        self.pqPreKeyRecords = pqPreKeyRecords
    }

    public func getSignedPreKey() -> LibZonaRosaClient.SignedPreKeyRecord? { signedPreKey }
    public func getPreKeyRecords() -> [LibZonaRosaClient.PreKeyRecord]? { preKeyRecords }
    public func getLastResortPreKey() -> LibZonaRosaClient.KyberPreKeyRecord? { lastResortPreKey }
    public func getPqPreKeyRecords() -> [LibZonaRosaClient.KyberPreKeyRecord]? { pqPreKeyRecords }
}

public final class RegistrationPreKeyUploadBundle: PreKeyUploadBundle {
    public let identity: OWSIdentity
    public let identityKeyPair: ECKeyPair
    public let signedPreKey: LibZonaRosaClient.SignedPreKeyRecord
    public let lastResortPreKey: LibZonaRosaClient.KyberPreKeyRecord

    public init(
        identity: OWSIdentity,
        identityKeyPair: ECKeyPair,
        signedPreKey: LibZonaRosaClient.SignedPreKeyRecord,
        lastResortPreKey: LibZonaRosaClient.KyberPreKeyRecord,
    ) {
        self.identity = identity
        self.identityKeyPair = identityKeyPair
        self.signedPreKey = signedPreKey
        self.lastResortPreKey = lastResortPreKey
    }

    public func getSignedPreKey() -> LibZonaRosaClient.SignedPreKeyRecord? { signedPreKey }
    public func getPreKeyRecords() -> [LibZonaRosaClient.PreKeyRecord]? { nil }
    public func getLastResortPreKey() -> LibZonaRosaClient.KyberPreKeyRecord? { lastResortPreKey }
    public func getPqPreKeyRecords() -> [LibZonaRosaClient.KyberPreKeyRecord]? { nil }
}

public struct RegistrationPreKeyUploadBundles {
    public let aci: RegistrationPreKeyUploadBundle
    public let pni: RegistrationPreKeyUploadBundle

    public init(aci: RegistrationPreKeyUploadBundle, pni: RegistrationPreKeyUploadBundle) {
        self.aci = aci
        self.pni = pni
    }
}
