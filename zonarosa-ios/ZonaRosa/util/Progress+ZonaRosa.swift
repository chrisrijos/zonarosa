//
// Copyright 2023 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import Foundation

extension Progress {
    public var remainingUnitCount: Int64 { totalUnitCount - completedUnitCount }
}
