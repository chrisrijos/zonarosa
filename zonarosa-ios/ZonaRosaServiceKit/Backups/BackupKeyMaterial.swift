//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import LibZonaRosaClient

public protocol BackupKeyMaterial {
    var credentialType: BackupAuthCredentialType { get }
    var backupKey: BackupKey { get }

    func serialize() -> Data
    func deriveEcKey(aci: Aci) -> PrivateKey
    func deriveBackupId(aci: Aci) -> Data
}

extension BackupKeyMaterial {
    public func deriveEcKey(aci: Aci) -> PrivateKey {
        backupKey.deriveEcKey(aci: aci)
    }

    public func deriveBackupId(aci: Aci) -> Data {
        backupKey.deriveBackupId(aci: aci)
    }

    public func serialize() -> Data { backupKey.serialize() }
}

public enum BackupKeyMaterialError: Error {
    case missingMessageBackupKey
    case missingOrInvalidMRBK
    /// Encountered an error using libzonarosa methods to derive keys.
    case derivationError(Error)
}
