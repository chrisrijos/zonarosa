//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
public import LibZonaRosaClient

public struct MessageRootBackupKey: BackupKeyMaterial {
    public var credentialType: BackupAuthCredentialType { .messages }
    public let backupKey: BackupKey
    public let backupId: Data

    public let aci: Aci

    public init(accountEntropyPool: AccountEntropyPool, aci: Aci) throws(BackupKeyMaterialError) {
        do {
            let backupKey = try LibZonaRosaClient.AccountEntropyPool.deriveBackupKey(accountEntropyPool.rawString)
            self.init(backupKey: backupKey, aci: aci)
        } catch {
            throw BackupKeyMaterialError.derivationError(error)
        }
    }

    init(backupKey: BackupKey, aci: Aci) {
        self.backupKey = backupKey
        self.backupId = backupKey.deriveBackupId(aci: aci)
        self.aci = aci
    }
}
