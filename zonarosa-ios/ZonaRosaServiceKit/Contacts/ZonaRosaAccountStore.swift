//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public protocol ZonaRosaAccountStore {
    func fetchZonaRosaAccount(for rowId: ZonaRosaAccount.RowId, tx: DBReadTransaction) -> ZonaRosaAccount?
}

public class ZonaRosaAccountStoreImpl: ZonaRosaAccountStore {
    public init() {}

    public func fetchZonaRosaAccount(for rowId: ZonaRosaAccount.RowId, tx: DBReadTransaction) -> ZonaRosaAccount? {
        return SDSCodableModelDatabaseInterfaceImpl().fetchModel(modelType: ZonaRosaAccount.self, rowId: rowId, tx: tx)
    }
}
