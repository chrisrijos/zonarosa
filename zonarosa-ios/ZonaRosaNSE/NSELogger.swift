//
// Copyright 2022 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaServiceKit

class NSELogger: PrefixedLogger {
    static let uncorrelated = NSELogger(prefix: "uncorrelated")

    convenience init() {
        self.init(
            prefix: "[NSE]",
            suffix: "{{\(UUID().uuidString)}}",
        )
    }
}
