//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation

public extension Decimal {
    /// Round a decimal value to the nearest whole number.
    ///
    /// Uses ["plain" rounding][0] to break ties.
    ///
    /// - Returns: The decimal value roudned to the nearest whole number.
    ///
    /// [0]: https://developer.apple.com/documentation/foundation/nsdecimalnumber/roundingmode/plain
    func rounded() -> Decimal {
        let nsSelf = self as NSDecimalNumber
        let nsResult = nsSelf.rounding(accordingToBehavior: NSDecimalNumberHandler(
            roundingMode: .plain,
            scale: 0,
            raiseOnExactness: false,
            raiseOnOverflow: false,
            raiseOnUnderflow: false,
            raiseOnDivideByZero: false,
        ))
        return nsResult as Decimal
    }
}
