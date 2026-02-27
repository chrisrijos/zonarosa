//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public extension Decimal {
    /// Is this decimal a whole number?
    ///
    /// - Returns: `true` if the value is a whole number, `false` otherwise.
    var isInteger: Bool {
        (isZero || isNormal) && rounded() == self
    }
}
