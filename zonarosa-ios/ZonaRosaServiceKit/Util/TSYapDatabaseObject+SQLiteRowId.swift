//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public extension TSYapDatabaseObject {
    var sqliteRowId: Int64? {
        return grdbId?.int64Value
    }
}
